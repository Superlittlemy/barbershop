package com.slm.barbershop.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.BillConverter;
import com.slm.barbershop.entity.Bill;
import com.slm.barbershop.entity.BillItem;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.enums.BillPayChannel;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.lock.DistributedLock;
import com.slm.barbershop.mapper.BillItemMapper;
import com.slm.barbershop.mapper.BillMapper;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.mapper.ServiceItemMapper;
import com.slm.barbershop.model.BillPageVO;
import com.slm.barbershop.model.BillQuery;
import com.slm.barbershop.model.BillRequest;
import com.slm.barbershop.model.BillResponse;
import com.slm.barbershop.model.BillSummaryRowVO;
import com.slm.barbershop.model.BillSummaryVO;
import com.slm.barbershop.model.ShopBillRecentVO;
import com.slm.barbershop.model.TransactionItemRequest;
import com.slm.barbershop.model.TransactionItemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements com.slm.barbershop.service.BillService {

    @Autowired
    private BillMapper billMapper;

    @Autowired
    private BillItemMapper billItemMapper;

    @Autowired
    private BillConverter billConverter;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Autowired
    @Lazy
    private BillServiceImpl self;

    @Override
    public BillResponse create(BillRequest request) {
        if (request == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "请求不能为空");
        }
        if (!StringUtils.hasText(request.getIdempotencyKey())) {
            request.setIdempotencyKey(UUID.randomUUID().toString());
        }
        // 幂等前置
        Bill existed = findByIdempotencyKey(request.getIdempotencyKey());
        if (existed != null) {
            return toResponseWithItems(existed);
        }
        return self.doCreate(request);
    }

    @DistributedLock(key = "'bill_create:' + #request.shopId", mode = DistributedLock.Mode.REDIS)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public BillResponse doCreate(BillRequest request) {
        // 锁内再次幂等检查
        Bill existed = findByIdempotencyKey(request.getIdempotencyKey());
        if (existed != null) {
            return toResponseWithItems(existed);
        }

        BillPayChannel channel = BillPayChannel.of(request.getPayChannel());
        if (channel == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "支付方式无效:" + request.getPayChannel());
        }

        // 客户信息
        String customerName = request.getCustomerName();
        String customerPhone = request.getCustomerPhone();
        if (channel == BillPayChannel.MEMBER) {
            if (request.getMemberId() == null) {
                throw new BizException(HttpStatus.BAD_REQUEST, "会员划账时 memberId 必填");
            }
        } else {
            if (!StringUtils.hasText(customerName)) {
                throw new BizException(HttpStatus.BAD_REQUEST, "非会员场景客户姓名必填");
            }
            if (customerName.length() > 50) {
                throw new BizException(HttpStatus.BAD_REQUEST, "客户姓名长度不能超过50");
            }
            if (StringUtils.hasText(customerPhone) && customerPhone.length() > 20) {
                throw new BizException(HttpStatus.BAD_REQUEST, "客户手机号长度不能超过20");
            }
        }

        // 校验会员归属
        Member member = null;
        if (channel == BillPayChannel.MEMBER) {
            member = memberMapper.selectById(request.getMemberId());
            if (member == null) {
                throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
            }
            if (!member.getShopId().equals(request.getShopId())) {
                throw new BizException(HttpStatus.FORBIDDEN, "会员不属于当前店铺");
            }
        }

        // 校验项目 + 累计金额
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "账单必须包含至少一个服务项目");
        }
        Set<Long> itemIds = request.getItems().stream()
                .map(TransactionItemRequest::getItemId)
                .collect(Collectors.toSet());
        List<ServiceItem> matched = lookupServiceItems(request.getShopId(), itemIds);
        Map<Long, ServiceItem> itemMap = matched.stream()
                .collect(Collectors.toMap(ServiceItem::getId, i -> i, (a, b) -> a));

        BigDecimal total = BigDecimal.ZERO;
        List<BillItem> persistItems = new ArrayList<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            TransactionItemRequest req = request.getItems().get(i);
            if (req.getItemId() == null) {
                throw new BizException(HttpStatus.BAD_REQUEST, "服务项目ID不能为空");
            }
            int qty = req.getQuantity() == null ? 1 : req.getQuantity();
            if (qty < 1 || qty > 999) {
                throw new BizException(HttpStatus.BAD_REQUEST, "服务项目数量必须在 1~999 之间");
            }
            ServiceItem item = itemMap.get(req.getItemId());
            if (item == null) {
                throw new BizException(HttpStatus.BAD_REQUEST, "服务项目不存在或不属于当前店铺");
            }
            if (item.getStatus() == null || item.getStatus() != 1) {
                throw new BizException(HttpStatus.BAD_REQUEST, "服务项目已下架:" + item.getName());
            }
            BigDecimal unitPrice = req.getUnitPrice() != null ? req.getUnitPrice() : item.getPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(HttpStatus.BAD_REQUEST, "服务项目价格无效:" + item.getName());
            }
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            total = total.add(subtotal);

            BillItem bi = new BillItem();
            bi.setItemId(item.getId());
            bi.setItemName(item.getName());
            bi.setUnitPrice(unitPrice);
            bi.setQuantity(qty);
            bi.setSubtotal(subtotal);
            bi.setSortNo(i);
            bi.setCreatedTime(LocalDateTime.now());
            persistItems.add(bi);
        }
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(HttpStatus.BAD_REQUEST, "账单总金额必须大于0");
        }

        // MEMBER 划账:扣余额
        if (channel == BillPayChannel.MEMBER) {
            BigDecimal balanceBefore = member.getBalance();
            if (balanceBefore.compareTo(total) < 0) {
                throw new BizException(HttpStatus.BAD_REQUEST, "余额不足");
            }
            BigDecimal balanceAfter = balanceBefore.subtract(total);
            member.setBalance(balanceAfter);
            int rows = memberMapper.updateById(member);
            if (rows == 0) {
                throw new OptimisticLockingFailureException(
                        "member balance update conflict, memberId=" + request.getMemberId());
            }
        }

        // 写账单主表
        Bill bill = new Bill();
        bill.setShopId(request.getShopId());
        bill.setMemberId(request.getMemberId());
        bill.setCustomerName(customerName);
        bill.setCustomerPhone(customerPhone);
        bill.setPayChannel(channel.name());
        bill.setTotalAmount(total);
        bill.setRemark(request.getRemark());
        bill.setIsCancelled(0);
        bill.setIdempotencyKey(request.getIdempotencyKey());
        try {
            billMapper.insert(bill);
        } catch (DuplicateKeyException e) {
            // 事务回滚,余额一并撤销
            throw new BizException(HttpStatus.CONFLICT, "重复提交");
        }

        // 写明细
        for (BillItem bi : persistItems) {
            bi.setBillId(bill.getId());
            billItemMapper.insert(bi);
        }

        return billConverter.toResponseWithItems(bill, toItemResponseList(persistItems));
    }

    @Override
    public BillResponse cancel(Long shopId, Long billId, String reason) {
        if (shopId == null || billId == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "参数不能为空");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BizException(HttpStatus.BAD_REQUEST, "作废原因不能为空");
        }
        return self.doCancel(shopId, billId, reason);
    }

    @DistributedLock(key = "'bill_cancel:' + #shopId + ':' + #billId", mode = DistributedLock.Mode.REDIS)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public BillResponse doCancel(Long shopId, Long billId, String reason) {
        Bill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "账单不存在");
        }
        if (!bill.getShopId().equals(shopId)) {
            throw new BizException(HttpStatus.FORBIDDEN, "账单不属于当前店铺");
        }
        if (bill.getIsCancelled() != null && bill.getIsCancelled() == 1) {
            throw new BizException(HttpStatus.BAD_REQUEST, "账单已作废");
        }

        BillPayChannel channel = BillPayChannel.of(bill.getPayChannel());
        if (channel == BillPayChannel.MEMBER) {
            if (bill.getMemberId() == null) {
                throw new BizException(HttpStatus.BAD_REQUEST, "账单无关联会员,无法回退余额");
            }
            Member member = memberMapper.selectById(bill.getMemberId());
            if (member == null) {
                throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
            }
            member.setBalance(member.getBalance().add(bill.getTotalAmount()));
            int rows = memberMapper.updateById(member);
            if (rows == 0) {
                throw new OptimisticLockingFailureException(
                        "member balance rollback conflict, memberId=" + bill.getMemberId());
            }
        }

        bill.setIsCancelled(1);
        bill.setCancelledTime(LocalDateTime.now());
        bill.setCancelReason(reason);
        int rows = billMapper.updateById(bill);
        if (rows == 0) {
            throw new OptimisticLockingFailureException("bill cancel CAS failed, billId=" + billId);
        }
        return toResponseWithItems(bill);
    }

    @Override
    public BillPageVO page(Long shopId, IPage<Bill> page, BillQuery query) {
        if (shopId == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "shopId 不能为空");
        }
        if (query == null) {
            query = new BillQuery();
        }
        long p = Math.max(page.getCurrent(), 1);
        long s = Math.min(Math.max(page.getSize(), 1), 100);
        Page<Bill> mpPage = new Page<>(p, s);
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<Bill>()
                .eq(Bill::getShopId, shopId)
                .orderByDesc(Bill::getCreatedTime)
                .orderByDesc(Bill::getId);
        if (!query.includeCancelled()) {
            wrapper.eq(Bill::getIsCancelled, 0);
        }
        if (StringUtils.hasText(query.getPayChannel())) {
            wrapper.eq(Bill::getPayChannel, query.getPayChannel());
        }
        if (query.getMemberId() != null) {
            wrapper.eq(Bill::getMemberId, query.getMemberId());
        }
        if (StringUtils.hasText(query.getCustomerName())) {
            wrapper.like(Bill::getCustomerName, query.getCustomerName());
        }
        if (StringUtils.hasText(query.getCustomerPhone())) {
            wrapper.like(Bill::getCustomerPhone, query.getCustomerPhone());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(Bill::getCreatedTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(Bill::getCreatedTime, query.getEndTime());
        }
        IPage<Bill> result = billMapper.selectPage(mpPage, wrapper);
        List<BillResponse> records = toResponseListWithItems(result.getRecords());
        long total = result.getTotal();
        boolean hasMore = p * s < total;
        return new BillPageVO(records, total, p, s, hasMore);
    }

    @Override
    public BillSummaryVO summary(Long shopId) {
        if (shopId == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "shopId 不能为空");
        }
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        List<BillSummaryRowVO> rows = billMapper.summarizeByShopId(shopId, todayStart, monthStart);

        BillSummaryVO vo = new BillSummaryVO();
        Map<String, BigDecimal> byChannelAmount = new LinkedHashMap<>();
        Map<String, Long> byChannelCount = new LinkedHashMap<>();
        // 初始化所有渠道为 0,保证前端展示稳定
        for (BillPayChannel c : BillPayChannel.values()) {
            byChannelAmount.put(c.name(), BigDecimal.ZERO);
            byChannelCount.put(c.name(), 0L);
        }
        BigDecimal todayAmount = BigDecimal.ZERO;
        long todayCount = 0L;
        BigDecimal monthAmount = BigDecimal.ZERO;
        long monthCount = 0L;
        for (BillSummaryRowVO row : rows) {
            String channel = row.getPayChannel();
            byChannelAmount.put(channel,
                    byChannelAmount.getOrDefault(channel, BigDecimal.ZERO)
                            .add(row.getAmount() == null ? BigDecimal.ZERO : row.getAmount()));
            byChannelCount.put(channel,
                    byChannelCount.getOrDefault(channel, 0L) + (row.getCount() == null ? 0L : row.getCount()));
            if ("TODAY".equals(row.getBucket())) {
                todayAmount = todayAmount.add(row.getAmount() == null ? BigDecimal.ZERO : row.getAmount());
                todayCount += row.getCount() == null ? 0L : row.getCount();
            } else if ("MONTH".equals(row.getBucket())) {
                monthAmount = monthAmount.add(row.getAmount() == null ? BigDecimal.ZERO : row.getAmount());
                monthCount += row.getCount() == null ? 0L : row.getCount();
            }
        }
        vo.setTodayAmount(todayAmount);
        vo.setTodayCount(todayCount);
        vo.setMonthAmount(monthAmount);
        vo.setMonthCount(monthCount);
        vo.setByChannelAmount(byChannelAmount);
        vo.setByChannelCount(byChannelCount);
        return vo;
    }

    @Override
    public BillResponse getDetail(Long shopId, Long billId) {
        if (shopId == null || billId == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "参数不能为空");
        }
        Bill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "账单不存在");
        }
        if (!bill.getShopId().equals(shopId)) {
            throw new BizException(HttpStatus.FORBIDDEN, "账单不属于当前店铺");
        }
        return toResponseWithItems(bill);
    }

    @Override
    public List<ShopBillRecentVO> listRecentByShopId(Long shopId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return billMapper.listRecentByShopId(shopId, safeLimit);
    }

    // ====== 私有辅助 ======

    private Bill findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            return null;
        }
        return billMapper.selectOne(
                new LambdaQueryWrapper<Bill>()
                        .eq(Bill::getIdempotencyKey, idempotencyKey)
                        .last("LIMIT 1")
        );
    }

    private List<ServiceItem> lookupServiceItems(Long shopId, Set<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getShopId, shopId)
                        .in(ServiceItem::getId, itemIds)
        );
    }

    private BillResponse toResponseWithItems(Bill bill) {
        List<TransactionItemResponse> items = listItemsByBillId(bill.getId());
        return billConverter.toResponseWithItems(bill, items);
    }

    private List<TransactionItemResponse> listItemsByBillId(Long billId) {
        if (billId == null) {
            return Collections.emptyList();
        }
        List<BillItem> items = billItemMapper.selectList(
                new LambdaQueryWrapper<BillItem>()
                        .eq(BillItem::getBillId, billId)
                        .orderByAsc(BillItem::getSortNo)
                        .orderByAsc(BillItem::getId)
        );
        return toItemResponseList(items);
    }

    private List<TransactionItemResponse> toItemResponseList(List<BillItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<TransactionItemResponse> out = new ArrayList<>(items.size());
        for (BillItem bi : items) {
            TransactionItemResponse r = new TransactionItemResponse();
            r.setId(bi.getId());
            r.setTransactionId(bi.getBillId());
            r.setItemId(bi.getItemId());
            r.setItemName(bi.getItemName());
            r.setUnitPrice(bi.getUnitPrice());
            r.setQuantity(bi.getQuantity());
            r.setSubtotal(bi.getSubtotal());
            r.setSortNo(bi.getSortNo());
            out.add(r);
        }
        return out;
    }

    private List<BillResponse> toResponseListWithItems(List<Bill> bills) {
        if (bills == null || bills.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> billIds = bills.stream().map(Bill::getId).collect(Collectors.toList());
        Map<Long, List<TransactionItemResponse>> itemsByBillId = new HashMap<>();
        List<BillItem> all = billItemMapper.selectList(
                new LambdaQueryWrapper<BillItem>()
                        .in(BillItem::getBillId, billIds)
                        .orderByAsc(BillItem::getSortNo)
                        .orderByAsc(BillItem::getId)
        );
        for (BillItem bi : all) {
            itemsByBillId.computeIfAbsent(bi.getBillId(), k -> new ArrayList<>())
                    .add(toItemResponse(bi));
        }
        List<BillResponse> responses = new ArrayList<>(bills.size());
        for (Bill b : bills) {
            BillResponse r = billConverter.toResponse(b);
            r.setItems(itemsByBillId.getOrDefault(b.getId(), Collections.emptyList()));
            responses.add(r);
        }
        return responses;
    }

    private TransactionItemResponse toItemResponse(BillItem bi) {
        TransactionItemResponse r = new TransactionItemResponse();
        r.setId(bi.getId());
        r.setTransactionId(bi.getBillId());
        r.setItemId(bi.getItemId());
        r.setItemName(bi.getItemName());
        r.setUnitPrice(bi.getUnitPrice());
        r.setQuantity(bi.getQuantity());
        r.setSubtotal(bi.getSubtotal());
        r.setSortNo(bi.getSortNo());
        return r;
    }

}
