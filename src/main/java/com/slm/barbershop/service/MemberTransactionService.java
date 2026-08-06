package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.MemberTransactionConverter;
import com.slm.barbershop.entity.Bill;
import com.slm.barbershop.entity.BillItem;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.entity.MemberTransaction;
import com.slm.barbershop.entity.MemberTransactionItem;
import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.enums.BillPayChannel;
import com.slm.barbershop.enums.BillType;
import com.slm.barbershop.enums.TransactionType;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.lock.DistributedLock;
import com.slm.barbershop.mapper.BillItemMapper;
import com.slm.barbershop.mapper.BillMapper;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.mapper.MemberTransactionItemMapper;
import com.slm.barbershop.mapper.MemberTransactionMapper;
import com.slm.barbershop.mapper.ServiceItemMapper;
import com.slm.barbershop.model.MemberTransactionQuery;
import com.slm.barbershop.model.MemberTransactionResponse;
import com.slm.barbershop.model.PageResult;
import com.slm.barbershop.model.ShopTransactionRecentVO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MemberTransactionService extends ServiceImpl<MemberTransactionMapper, MemberTransaction> {

    @Autowired
    private MemberTransactionMapper transactionMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberTransactionConverter transactionConverter;

    @Autowired
    private MemberTransactionItemMapper transactionItemMapper;

    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Autowired
    private BillMapper billMapper;

    @Autowired
    private BillItemMapper billItemMapper;

    /**
     * 注入自身代理,使 {@link #doUpdate} 上的 @Transactional 通过代理生效
     */
    @Autowired
    @Lazy
    private MemberTransactionService self;

    public MemberTransaction store(Long memberId, BigDecimal amount, String remark, String idempotencyKey) {
        return applyBalance(memberId, amount, TransactionType.STORE, remark, null, idempotencyKey);
    }

    public MemberTransaction consume(Long memberId,
                                     BigDecimal amount,
                                     String remark,
                                     List<TransactionItemRequest> items,
                                     String idempotencyKey) {
        return applyBalance(memberId, amount, TransactionType.CONSUME, remark, items, idempotencyKey);
    }

    /**
     * 公共余额变更流程:
     * 1. 幂等前置查询(命中 → 直接返回原流水)
     * 2. 走锁策略,在事务内重试:读 member → 校验 items/计算总额 → updateById(CAS) → 写流水 → 写明细
     * <p>
     * 消费时同时写 bill/bill_item(支付方式 MEMBER),通过 member_transaction.bill_id 反向关联;
     * MemberTransactionItem 不再写入,明细统一存到 bill_item。
     */
    private MemberTransaction applyBalance(Long memberId,
                                           BigDecimal amount,
                                           TransactionType type,
                                           String remark,
                                           List<TransactionItemRequest> items,
                                           String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            MemberTransaction existed = findByIdempotencyKey(idempotencyKey);
            if (existed != null) {
                return existed;
            }
        }

        return self.doUpdate(memberId, amount, type, remark, items, idempotencyKey);
    }

    /**
     * 单次带事务的余额变更;乐观锁冲突时抛 {@link OptimisticLockingFailureException} 由策略重试
     */
    @DistributedLock(key = "'member_transaction:' + #memberId", mode = DistributedLock.Mode.REDIS)
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public MemberTransaction doUpdate(Long memberId,
                                      BigDecimal amount,
                                      TransactionType type,
                                      String remark,
                                      List<TransactionItemRequest> items,
                                      String idempotencyKey) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
        }

        // 1) 若消费带 items,按项目汇总金额并校验;同时构造 bill_item 列表
        BigDecimal finalAmount = amount;
        List<TransactionItemRequest> effectiveItems = items == null ? Collections.emptyList() : items;
        List<BillItem> persistBillItems = new ArrayList<>();
        if (type == TransactionType.CONSUME) {
            if (effectiveItems.isEmpty()) {
                throw new BizException(HttpStatus.BAD_REQUEST, "消费必须包含至少一个服务项目");
            }
            finalAmount = BigDecimal.ZERO;
            Set<Long> itemIds = effectiveItems.stream()
                    .map(TransactionItemRequest::getItemId)
                    .collect(Collectors.toSet());
            List<ServiceItem> matched = lookupServiceItems(member.getShopId(), itemIds);
            Map<Long, ServiceItem> itemMap = matched.stream()
                    .collect(Collectors.toMap(ServiceItem::getId, i -> i, (a, b) -> a));
            for (int i = 0; i < effectiveItems.size(); i++) {
                TransactionItemRequest req = effectiveItems.get(i);
                if (req.getItemId() == null) {
                    throw new BizException(HttpStatus.BAD_REQUEST, "消费项目ID不能为空");
                }
                Integer qty = req.getQuantity() == null ? 1 : req.getQuantity();
                if (qty < 1 || qty > 999) {
                    throw new BizException(HttpStatus.BAD_REQUEST, "消费项目数量必须在 1~999 之间");
                }
                ServiceItem item = itemMap.get(req.getItemId());
                if (item == null) {
                    throw new BizException(HttpStatus.BAD_REQUEST, "消费项目不存在或不属于当前店铺");
                }
                if (item.getStatus() == null || item.getStatus() != 1) {
                    throw new BizException(HttpStatus.BAD_REQUEST, "消费项目已下架:" + item.getName());
                }
                BigDecimal unitPrice = req.getUnitPrice() != null ? req.getUnitPrice() : item.getPrice();
                if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BizException(HttpStatus.BAD_REQUEST, "消费项目价格无效:" + item.getName());
                }
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                finalAmount = finalAmount.add(subtotal);

                BillItem bi = new BillItem();
                bi.setItemId(item.getId());
                bi.setItemName(item.getName());
                bi.setUnitPrice(unitPrice);
                bi.setQuantity(qty);
                bi.setSubtotal(subtotal);
                bi.setSortNo(i);
                bi.setCreatedTime(LocalDateTime.now());
                persistBillItems.add(bi);
            }
        }

        if (finalAmount == null || finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(HttpStatus.BAD_REQUEST, "交易金额必须大于0");
        }

        // 2) 计算余额变化
        BigDecimal balanceBefore = member.getBalance();
        BigDecimal balanceAfter;
        if (type == TransactionType.CONSUME) {
            if (balanceBefore.compareTo(finalAmount) < 0) {
                throw new BizException(HttpStatus.BAD_REQUEST, "余额不足");
            }
            balanceAfter = balanceBefore.subtract(finalAmount);
        } else {
            balanceAfter = balanceBefore.add(finalAmount);
        }

        member.setBalance(balanceAfter);
        int rows = memberMapper.updateById(member);
        if (rows == 0) {
            // @Version 触发 CAS 失败,抛出让策略重试
            throw new OptimisticLockingFailureException(
                    "member balance update conflict, memberId=" + memberId);
        }

        // 3) 写交易主表
        MemberTransaction transaction = new MemberTransaction();
        transaction.setMemberId(memberId);
        transaction.setType(type.name());
        transaction.setAmount(finalAmount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark(remark);
        transaction.setIdempotencyKey(idempotencyKey);
        try {
            transactionMapper.insert(transaction);
        } catch (DuplicateKeyException e) {
            // 并发同 idempotencyKey,事务回滚,余额一并撤销
            throw new BizException(HttpStatus.CONFLICT, "重复提交");
        }

        // 4) 同步写 bill:
        //    - 消费: bill(type=CONSUME, pay_channel=MEMBER)+ bill_item
        //    - 储值: bill(type=STORE, pay_channel=OFFLINE),不带 bill_item
        //    两者都把 bill_id 回填到 member_transaction,关联账单与流水
        if (type == TransactionType.CONSUME && !persistBillItems.isEmpty()) {
            Bill bill = new Bill();
            bill.setShopId(member.getShopId());
            bill.setMemberId(memberId);
            // 会员姓名/手机号快照(便于账单列表"客户"列展示,即使会员改名也不影响历史账单)
            bill.setCustomerName(member.getName());
            bill.setCustomerPhone(member.getPhone());
            bill.setPayChannel(BillPayChannel.MEMBER.name());
            bill.setType(BillType.CONSUME.name());
            bill.setTotalAmount(finalAmount);
            bill.setRemark(remark);
            bill.setIsCancelled(0);
            // 复用同一幂等键(uniq 冲突时事务回滚,余额一并撤销)
            bill.setIdempotencyKey(idempotencyKey);
            try {
                billMapper.insert(bill);
            } catch (DuplicateKeyException e) {
                throw new BizException(HttpStatus.CONFLICT, "重复提交");
            }
            for (BillItem bi : persistBillItems) {
                bi.setBillId(bill.getId());
                billItemMapper.insert(bi);
            }
            transaction.setBillId(bill.getId());
            transactionMapper.updateById(transaction);
        } else if (type == TransactionType.STORE) {
            Bill bill = new Bill();
            bill.setShopId(member.getShopId());
            bill.setMemberId(memberId);
            // 储值账单也用会员姓名/手机号快照,便于列表展示
            bill.setCustomerName(member.getName());
            bill.setCustomerPhone(member.getPhone());
            // 储值默认 OFFLINE(用户通过店铺后台手工登记的储值,默认线下收款)
            bill.setPayChannel(BillPayChannel.OFFLINE.name());
            bill.setType(BillType.STORE.name());
            bill.setTotalAmount(finalAmount);
            bill.setRemark(remark);
            bill.setIsCancelled(0);
            // 复用同一幂等键
            bill.setIdempotencyKey(idempotencyKey);
            try {
                billMapper.insert(bill);
            } catch (DuplicateKeyException e) {
                throw new BizException(HttpStatus.CONFLICT, "重复提交");
            }
            // 储值不带 bill_item(没有服务项目)
            transaction.setBillId(bill.getId());
            transactionMapper.updateById(transaction);
        }

        return transaction;
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

    private MemberTransaction findByIdempotencyKey(String idempotencyKey) {
        return transactionMapper.selectOne(
                new LambdaQueryWrapper<MemberTransaction>()
                        .eq(MemberTransaction::getIdempotencyKey, idempotencyKey)
                        .last("LIMIT 1")
        );
    }

    public BigDecimal getBalance(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        return member.getBalance();
    }

    /**
     * 读取某一笔交易的项目明细(用于消费接口返回 items 字段)。
     * <p>
     * 优先通过 bill_id 查询 bill_item(新数据);若 bill_id 为空(历史/储值)则回退到 member_transaction_item。
     */
    public List<TransactionItemResponse> listItemsByTransactionId(Long transactionId) {
        if (transactionId == null) {
            return Collections.emptyList();
        }
        MemberTransaction tx = transactionMapper.selectById(transactionId);
        if (tx == null) {
            return Collections.emptyList();
        }
        if (tx.getBillId() != null) {
            return listBillItemsByBillId(tx.getBillId());
        }
        // 历史/储值场景:回退到 member_transaction_item
        return listLegacyItemsByTransactionId(transactionId);
    }

    public List<MemberTransactionResponse> listByMemberId(Long memberId) {
        List<MemberTransaction> records = transactionMapper.selectList(
                new LambdaQueryWrapper<MemberTransaction>()
                        .eq(MemberTransaction::getMemberId, memberId)
                        .orderByDesc(MemberTransaction::getCreatedTime)
        );
        return toResponseListWithItems(records);
    }

    /**
     * 按店铺查询最近 N 条交易记录(用于店铺详情页右侧"最新动态")。
     * limit 上限 50,避免一次拉太多。
     */
    public List<ShopTransactionRecentVO> listRecentByShopId(Long shopId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return transactionMapper.listRecentByShopId(shopId, safeLimit);
    }

    /**
     * 分页查询交易流水,默认按 created_time DESC, id DESC 排序(支持前端 sort 覆盖)。
     * page 从 1 开始,size 默认 20,上限 100。
     */
    public PageResult<MemberTransactionResponse> pageByMemberId(Long memberId, MemberTransactionQuery query) {
        LambdaQueryWrapper<MemberTransaction> wrapper = new LambdaQueryWrapper<MemberTransaction>()
                .eq(MemberTransaction::getMemberId, memberId);
        IPage<MemberTransaction> result = transactionMapper.selectPage(query, wrapper);
        List<MemberTransactionResponse> responseList = toResponseListWithItems(result.getRecords());
        return PageResult.of(result, responseList);
    }

    /**
     * 将交易列表转换为带 items 的响应列表。
     * 新数据通过 bill_id 批量查 bill_item;bill_id 为空的(历史/储值)回退到 member_transaction_item。
     */
    private List<MemberTransactionResponse> toResponseListWithItems(List<MemberTransaction> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<TransactionItemResponse>> itemsByTxId = new HashMap<>();

        // 1) 新数据:按 bill_id 批量查 bill_item
        List<Long> billIds = records.stream()
                .map(MemberTransaction::getBillId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (!billIds.isEmpty()) {
            List<BillItem> billItems = billItemMapper.selectList(
                    new LambdaQueryWrapper<BillItem>()
                            .in(BillItem::getBillId, billIds)
                            .orderByAsc(BillItem::getSortNo)
                            .orderByAsc(BillItem::getId)
            );
            // 需要反向把 bill_id 映射回 transaction_id,以便合并到 itemsByTxId
            Map<Long, Long> billIdToTxId = new HashMap<>();
            for (MemberTransaction tx : records) {
                if (tx.getBillId() != null) {
                    billIdToTxId.put(tx.getBillId(), tx.getId());
                }
            }
            for (BillItem bi : billItems) {
                Long txId = billIdToTxId.get(bi.getBillId());
                if (txId == null) continue;
                itemsByTxId.computeIfAbsent(txId, k -> new ArrayList<>())
                        .add(toItemResponse(bi));
            }
        }

        // 2) 历史/储值:回退到 member_transaction_item
        List<Long> legacyTxIds = records.stream()
                .filter(r -> r.getBillId() == null)
                .map(MemberTransaction::getId)
                .collect(Collectors.toList());
        if (!legacyTxIds.isEmpty()) {
            List<MemberTransactionItem> allItems = transactionItemMapper.selectList(
                    new LambdaQueryWrapper<MemberTransactionItem>()
                            .in(MemberTransactionItem::getTransactionId, legacyTxIds)
                            .orderByAsc(MemberTransactionItem::getSortNo)
                            .orderByAsc(MemberTransactionItem::getId)
            );
            for (MemberTransactionItem mi : allItems) {
                itemsByTxId.computeIfAbsent(mi.getTransactionId(), k -> new ArrayList<>())
                        .add(toItemResponse(mi));
            }
        }

        List<MemberTransactionResponse> responses = new ArrayList<>(records.size());
        for (MemberTransaction tx : records) {
            MemberTransactionResponse r = transactionConverter.toResponse(tx);
            r.setItems(itemsByTxId.getOrDefault(tx.getId(), Collections.emptyList()));
            responses.add(r);
        }
        return responses;
    }

    private List<TransactionItemResponse> listBillItemsByBillId(Long billId) {
        if (billId == null) {
            return Collections.emptyList();
        }
        List<BillItem> items = billItemMapper.selectList(
                new LambdaQueryWrapper<BillItem>()
                        .eq(BillItem::getBillId, billId)
                        .orderByAsc(BillItem::getSortNo)
                        .orderByAsc(BillItem::getId)
        );
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<TransactionItemResponse> responses = new ArrayList<>(items.size());
        for (BillItem bi : items) {
            responses.add(toItemResponse(bi));
        }
        return responses;
    }

    private List<TransactionItemResponse> listLegacyItemsByTransactionId(Long transactionId) {
        List<MemberTransactionItem> items = transactionItemMapper.selectList(
                new LambdaQueryWrapper<MemberTransactionItem>()
                        .eq(MemberTransactionItem::getTransactionId, transactionId)
                        .orderByAsc(MemberTransactionItem::getSortNo)
                        .orderByAsc(MemberTransactionItem::getId)
        );
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<TransactionItemResponse> responses = new ArrayList<>(items.size());
        for (MemberTransactionItem mi : items) {
            responses.add(toItemResponse(mi));
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

    private TransactionItemResponse toItemResponse(MemberTransactionItem mi) {
        TransactionItemResponse r = new TransactionItemResponse();
        r.setId(mi.getId());
        r.setTransactionId(mi.getTransactionId());
        r.setItemId(mi.getItemId());
        r.setItemName(mi.getItemName());
        r.setUnitPrice(mi.getUnitPrice());
        r.setQuantity(mi.getQuantity());
        r.setSubtotal(mi.getSubtotal());
        r.setSortNo(mi.getSortNo());
        return r;
    }

}