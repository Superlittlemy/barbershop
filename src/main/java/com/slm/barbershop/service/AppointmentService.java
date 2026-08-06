package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.AppointmentConverter;
import com.slm.barbershop.entity.Appointment;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.enums.AppointmentStatus;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.AppointmentMapper;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.mapper.ServiceItemMapper;
import com.slm.barbershop.mapper.ShopMapper;
import com.slm.barbershop.model.AppointmentPageQuery;
import com.slm.barbershop.model.AppointmentRequest;
import com.slm.barbershop.model.AppointmentResponse;
import com.slm.barbershop.model.AuthUser;
import com.slm.barbershop.model.PageResult;
import com.slm.barbershop.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预约服务
 * <p>
 * 核心职责:
 * 1. 创建预约(含营业时间校验 + 时段冲突校验)
 * 2. 列出某店铺某日可用时段(1 小时间隔,末端不足 1 小时丢弃)
 * 3. 店家分页查询预约(回填 memberName / serviceItemName / shopName)
 */
@Service
public class AppointmentService extends ServiceImpl<AppointmentMapper, Appointment> {

    /** 预约时段固定 1 小时(分钟数) */
    private static final int SLOT_MINUTES = 60;

    /** 周内休息标记字符串中索引 0 对应的 dayOfWeek(1=周一) */
    private static final int WEEK_OFF_INDEX_BASE = 1;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private AppointmentConverter appointmentConverter;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Autowired
    private MemberMapper memberMapper;

    // ==================== 创建预约 ====================

    public Appointment create(AppointmentRequest request) {
        AuthUser auth = UserContext.getUser();
        if (auth == null) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (!auth.isMember()) {
            throw new BizException(HttpStatus.FORBIDDEN, "仅会员可创建预约");
        }
        Long memberId = auth.getId();
        LocalDate date = request.getAppointmentDate();
        LocalTime start = parseStartTime(request.getStartTime());
        LocalTime end = start.plusHours(1);

        // 1) 校验店铺存在 + 营业时间
        Shop shop = shopMapper.selectById(request.getShopId());
        if (shop == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "店铺不存在");
        }
        assertWithinBusinessHours(shop, date, start, end);
        // 2) 校验服务项目存在且上架,且属于该店铺
        ServiceItem item = serviceItemMapper.selectById(request.getServiceItemId());
        if (item == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "服务项目不存在");
        }
        if (!java.util.Objects.equals(item.getShopId(), request.getShopId())) {
            throw new BizException(HttpStatus.BAD_REQUEST, "服务项目不属于该店铺");
        }
        if (item.getStatus() == null || item.getStatus() != 1) {
            throw new BizException(HttpStatus.BAD_REQUEST, "服务项目已下架,无法预约");
        }
        // 3) 时段冲突校验(同人同时段)
        Long conflict = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getShopId, request.getShopId())
                .eq(Appointment::getMemberId, memberId)
                .eq(Appointment::getAppointmentDate, date)
                .eq(Appointment::getStartTime, start));
        if (conflict != null && conflict > 0) {
            throw new BizException(HttpStatus.CONFLICT, "该时段已被占用,请重新选择");
        }

        Appointment ap = new Appointment();
        ap.setShopId(request.getShopId());
        ap.setMemberId(memberId);
        ap.setServiceItemId(request.getServiceItemId());
        ap.setAppointmentDate(date);
        ap.setStartTime(start);
        ap.setEndTime(end);
        ap.setStatus(AppointmentStatus.VALID.getCode());
        ap.setRemark(request.getRemark());
        appointmentMapper.insert(ap);
        return ap;
    }

    /**
     * 校验 [start, end) 是否被店铺在指定日期的营业时间完全覆盖。
     * 单段非跨日设计:open_time <= start, end <= close_time,且该日期未标记为周内休息。
     */
    private void assertWithinBusinessHours(Shop shop, LocalDate date, LocalTime start, LocalTime end) {
        LocalTime open = shop.getOpenTime();
        LocalTime close = shop.getCloseTime();
        if (open == null || close == null) {
            throw new BizException(HttpStatus.UNPROCESSABLE_ENTITY, "店铺未配置营业时间");
        }
        int dow = date.getDayOfWeek().getValue();
        if (isWeeklyOff(shop.getWeeklyOff(), dow)) {
            throw new BizException(HttpStatus.UNPROCESSABLE_ENTITY, "所选日期店铺休息");
        }
        if (start.isBefore(open) || end.isAfter(close)) {
            throw new BizException(HttpStatus.UNPROCESSABLE_ENTITY, "所选时段不在营业时间内");
        }
    }

    /**
     * 检查 weeklyOff 字符串中 dayOfWeek(1=周一...7=周日)对应位是否为 1。
     */
    private boolean isWeeklyOff(String weeklyOff, int dayOfWeek) {
        if (weeklyOff == null || weeklyOff.length() < 7) return false;
        int idx = dayOfWeek - WEEK_OFF_INDEX_BASE;
        if (idx < 0 || idx >= 7) return false;
        return weeklyOff.charAt(idx) == '1';
    }

    private LocalTime parseStartTime(String s) {
        if (s == null || s.isBlank()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "起始时段不能为空");
        }
        String[] parts = s.split(":");
        if (parts.length < 2) {
            throw new BizException(HttpStatus.BAD_REQUEST, "起始时段格式错误(应为 HH:mm)");
        }
        try {
            int hh = Integer.parseInt(parts[0]);
            int mm = Integer.parseInt(parts[1]);
            return LocalTime.of(hh, mm);
        } catch (Exception e) {
            throw new BizException(HttpStatus.BAD_REQUEST, "起始时段格式错误(应为 HH:mm)");
        }
    }

    // ==================== 可用时段 ====================

    /**
     * 列出某店铺某日所有可预约时段(1 小时间隔)。
     * <p>
     * 规则:
     * - 末端不足 1 小时丢弃
     * - 已被预约占用的时段丢弃
     * - 早于"现在"的时段丢弃
     * - 周内休息日直接返回空列表
     */
    public List<String> listAvailableSlots(Long shopId, LocalDate date) {
        if (shopId == null || date == null) {
            return Collections.emptyList();
        }
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null || shop.getOpenTime() == null || shop.getCloseTime() == null) {
            return Collections.emptyList();
        }
        int dow = date.getDayOfWeek().getValue();
        if (isWeeklyOff(shop.getWeeklyOff(), dow)) {
            return Collections.emptyList();
        }

        Set<LocalTime> occupied = new HashSet<>(appointmentMapper.listStartTimes(shopId, date));
        LocalDateTime now = LocalDateTime.now();
        boolean isToday = date.equals(now.toLocalDate());
        LocalTime current = now.toLocalTime();

        LocalTime open = shop.getOpenTime();
        LocalTime close = shop.getCloseTime();
        long totalMin = java.time.Duration.between(open, close).toMinutes();
        long fullSlots = totalMin / SLOT_MINUTES;
        List<String> result = new ArrayList<>();
        for (long i = 0; i < fullSlots; i++) {
            LocalTime s = open.plusMinutes(i * SLOT_MINUTES);
            LocalTime e = s.plusMinutes(SLOT_MINUTES);
            if (e.isAfter(close)) break;
            if (isToday && s.isBefore(current)) continue;
            if (occupied.contains(s)) continue;
            result.add(String.format("%02d:%02d", s.getHour(), s.getMinute()));
        }
        return result;
    }

    // ==================== 店家分页查询 ====================

    public PageResult<AppointmentResponse> page(AppointmentPageQuery query) {
        long p = Math.max(query.getCurrent(), 1);
        long s = Math.min(Math.max(query.getSize(), 1), 100);
        Page<Appointment> mpPage = new Page<>(p, s);

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getShopId, query.getShopId())
                .eq(query.getDate() != null, Appointment::getAppointmentDate, query.getDate());

        String keyword = query.getKeyword();
        if (keyword != null && !keyword.isBlank()) {
            List<Long> memberIds = memberMapper.selectList(
                    new LambdaQueryWrapper<Member>()
                            .select(Member::getId)
                            .eq(Member::getShopId, query.getShopId())
                            .like(Member::getName, keyword))
                    .stream().map(Member::getId).collect(Collectors.toList());

            List<Long> serviceItemIds = serviceItemMapper.selectList(
                    new LambdaQueryWrapper<ServiceItem>()
                            .select(ServiceItem::getId)
                            .eq(ServiceItem::getShopId, query.getShopId())
                            .like(ServiceItem::getName, keyword))
                    .stream().map(ServiceItem::getId).collect(Collectors.toList());

            if (memberIds.isEmpty() && serviceItemIds.isEmpty()) {
                return PageResult.empty(p, s);
            }

            wrapper.and(w -> {
                if (!memberIds.isEmpty()) {
                    w.in(Appointment::getMemberId, memberIds);
                }
                if (!serviceItemIds.isEmpty()) {
                    if (!memberIds.isEmpty()) {
                        w.or();
                    }
                    w.in(Appointment::getServiceItemId, serviceItemIds);
                }
            });
        }

        IPage<Appointment> result = appointmentMapper.selectPage(mpPage, wrapper);
        return PageResult.of(result, toResponseListWithNames(result.getRecords()));
    }

    public AppointmentResponse toResponseWithNames(Appointment ap) {
        return fillNames(ap, appointmentConverter.toResponse(ap));
    }

    /**
     * 单条实体转 response,并回填 memberName / serviceItemName / shopName。
     * 供 controller 在创建后直接返回带友好字段的 response。
     */
    public AppointmentResponse fillNames(Appointment ap, AppointmentResponse resp) {
        if (ap == null) return resp;
        if (ap.getMemberId() != null) {
            Member m = memberMapper.selectById(ap.getMemberId());
            if (m != null) resp.setMemberName(m.getName());
        }
        if (ap.getServiceItemId() != null) {
            ServiceItem it = serviceItemMapper.selectById(ap.getServiceItemId());
            if (it != null) resp.setServiceItemName(it.getName());
        }
        if (ap.getShopId() != null) {
            Shop sh = shopMapper.selectById(ap.getShopId());
            if (sh != null) resp.setShopName(sh.getName());
        }
        return resp;
    }

    /**
     * 批量实体转 response,并一次性回填 memberName / serviceItemName / shopName。
     * <p>
     * 相比循环调用 {@link #fillNames} 的 N+1(selectById × N × 3)实现,本方法固定 3 次批量查询,
     * 适用于分页场景(单条 create 场景仍走 fillNames,避免无谓的批量查)。
     */
    private List<AppointmentResponse> toResponseListWithNames(List<Appointment> aps) {
        if (aps == null || aps.isEmpty()) return Collections.emptyList();

        Set<Long> memberIds = aps.stream().map(Appointment::getMemberId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> serviceItemIds = aps.stream().map(Appointment::getServiceItemId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> shopIds = aps.stream().map(Appointment::getShopId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> memberNames = memberMapper.selectBatchIds(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getName));
        Map<Long, String> serviceItemNames = serviceItemMapper.selectBatchIds(serviceItemIds).stream()
                .collect(Collectors.toMap(ServiceItem::getId, ServiceItem::getName));
        Map<Long, String> shopNames = shopMapper.selectBatchIds(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, Shop::getName));

        List<AppointmentResponse> out = new ArrayList<>(aps.size());
        for (Appointment ap : aps) {
            AppointmentResponse resp = appointmentConverter.toResponse(ap);
            if (ap.getMemberId() != null) resp.setMemberName(memberNames.get(ap.getMemberId()));
            if (ap.getServiceItemId() != null) resp.setServiceItemName(serviceItemNames.get(ap.getServiceItemId()));
            if (ap.getShopId() != null) resp.setShopName(shopNames.get(ap.getShopId()));
            out.add(resp);
        }
        return out;
    }

}
