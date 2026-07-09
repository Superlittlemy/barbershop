package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.AppointmentConverter;
import com.slm.barbershop.entity.Appointment;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.entity.ShopBusinessHours;
import com.slm.barbershop.enums.AppointmentStatus;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.AppointmentMapper;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.mapper.ServiceItemMapper;
import com.slm.barbershop.mapper.ShopMapper;
import com.slm.barbershop.model.AppointmentRequest;
import com.slm.barbershop.model.AppointmentResponse;
import com.slm.barbershop.model.AuthUser;
import com.slm.barbershop.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private AppointmentConverter appointmentConverter;

    @Autowired
    private ShopBusinessHoursService businessHoursService;

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

        // 1) 校验店铺存在
        Shop shop = shopMapper.selectById(request.getShopId());
        if (shop == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "店铺不存在");
        }
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
        // 3) 校验时段在营业时间内
        assertWithinBusinessHours(request.getShopId(), date, start, end);
        // 4) 时段冲突校验(同人同时段)
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
     * 跨日营业时间若 endTime < startTime,按 [startTime, 24:00) ∪ [00:00, endTime) 校验。
     */
    private void assertWithinBusinessHours(Long shopId, LocalDate date, LocalTime start, LocalTime end) {
        // ISO: 1=Mon..7=Sun,与 day_of_week 直接对应
        int dow = date.getDayOfWeek().getValue();
        List<ShopBusinessHours> hours = businessHoursService.listByShopId(shopId).stream()
                .filter(h -> h.getDayOfWeek() != null && h.getDayOfWeek() == dow)
                .collect(Collectors.toList());
        if (hours.isEmpty()) {
            throw new BizException(HttpStatus.UNPROCESSABLE_ENTITY, "所选日期店铺未营业");
        }
        for (ShopBusinessHours h : hours) {
            if (isCovered(h, start, end)) return;
        }
        throw new BizException(HttpStatus.UNPROCESSABLE_ENTITY, "所选时段不在营业时间内");
    }

    private boolean isCovered(ShopBusinessHours h, LocalTime start, LocalTime end) {
        LocalTime hs = h.getStartTime();
        LocalTime he = h.getEndTime();
        boolean cross = h.getCrossDay() != null && h.getCrossDay() == 1;
        if (!cross) {
            return !start.isBefore(hs) && !end.isAfter(he);
        }
        // 跨日:[hs, 24:00) ∪ [00:00, he)
        if (!start.isBefore(hs) && end.compareTo(LocalTime.MAX) <= 0 && end.isAfter(hs)) return true;
        if (!end.isAfter(he) && start.compareTo(LocalTime.MIN) >= 0 && start.isBefore(he)) return true;
        return false;
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
     */
    public List<String> listAvailableSlots(Long shopId, LocalDate date) {
        if (shopId == null || date == null) {
            return Collections.emptyList();
        }
        int dow = date.getDayOfWeek().getValue();
        List<ShopBusinessHours> hours = businessHoursService.listByShopId(shopId).stream()
                .filter(h -> h.getDayOfWeek() != null && h.getDayOfWeek() == dow)
                .collect(Collectors.toList());
        if (hours.isEmpty()) return Collections.emptyList();

        // 已被占用的起始时段
        Set<LocalTime> occupied = new HashSet<>(appointmentMapper.listStartTimes(shopId, date));
        // 过滤过期:今天之前全部过期;今天则只保留 >= 当前时间的格
        LocalDateTime now = LocalDateTime.now();
        boolean isToday = date.equals(now.toLocalDate());
        LocalTime current = now.toLocalTime();

        List<String> result = new ArrayList<>();
        for (ShopBusinessHours h : hours) {
            result.addAll(generateSlots(h, occupied, isToday, current));
        }
        Collections.sort(result);
        return result;
    }

    private List<String> generateSlots(ShopBusinessHours h, Set<LocalTime> occupied, boolean isToday, LocalTime current) {
        LocalTime hs = h.getStartTime();
        LocalTime he = h.getEndTime();
        boolean cross = h.getCrossDay() != null && h.getCrossDay() == 1;
        List<String> slots = new ArrayList<>();
        if (cross) {
            // 段一:[hs, 24:00);段二:[00:00, he)
            slots.addAll(sliceSlots(hs, LocalTime.MAX, occupied, isToday, current, true));
            slots.addAll(sliceSlots(LocalTime.MIN, he, occupied, isToday, current, false));
        } else {
            slots.addAll(sliceSlots(hs, he, occupied, isToday, current, false));
        }
        return slots;
    }

    /**
     * 在 [from, to) 区间生成整点起始时段(单位:SLOT_MINUTES)。
     * allowEndEqualsMax=true 时允许 to=24:00:00 的边界(用于跨日营业时间的第一段)。
     * 末端不足 1 小时直接丢弃。
     */
    private List<String> sliceSlots(LocalTime from, LocalTime to, Set<LocalTime> occupied,
                                    boolean isToday, LocalTime current, boolean allowEndEqualsMax) {
        if (from == null || to == null) return Collections.emptyList();
        long totalMin = Duration.between(from, to).toMinutes();
        if (totalMin < SLOT_MINUTES) return Collections.emptyList();
        long fullSlots = totalMin / SLOT_MINUTES;
        List<String> slots = new ArrayList<>();
        for (long i = 0; i < fullSlots; i++) {
            LocalTime s = from.plusMinutes(i * SLOT_MINUTES);
            LocalTime e = s.plusMinutes(SLOT_MINUTES);
            if (!allowEndEqualsMax && e.isAfter(to)) break;
            if (isToday && s.isBefore(current)) continue;
            if (occupied.contains(s)) continue;
            slots.add(String.format("%02d:%02d", s.getHour(), s.getMinute()));
        }
        return slots;
    }

    // ==================== 店家分页查询 ====================

    public IPage<AppointmentResponse> pageByShop(IPage<Appointment> page, Long shopId, LocalDate date, String keyword) {
        // 注:keyword 在本期极简版未参与 SQL 过滤(memberName/serviceItemName 在另一张表,
        // 引入 join 提升复杂度;为保持最小化改动,留待后续需求按 member_id/service_item_id IN 改造)。
        // 服务端拿到 keyword 后会在 controller 直接透传,前端"暂无匹配"由前端对当前页 records 做内存过滤呈现。
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getShopId, shopId)
                .eq(date != null, Appointment::getAppointmentDate, date)
                .orderByDesc(Appointment::getAppointmentDate)
                .orderByDesc(Appointment::getStartTime);
        IPage<Appointment> ap = appointmentMapper.selectPage(page, wrapper);
        return ap.convert(this::toResponseWithNames);
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

}
