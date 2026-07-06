package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.MemberTransactionConverter;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.entity.MemberTransaction;
import com.slm.barbershop.entity.MemberTransactionItem;
import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.enums.TransactionType;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.lock.DistributedLock;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.mapper.MemberTransactionItemMapper;
import com.slm.barbershop.mapper.MemberTransactionMapper;
import com.slm.barbershop.mapper.ServiceItemMapper;
import com.slm.barbershop.model.MemberTransactionPageVO;
import com.slm.barbershop.model.MemberTransactionResponse;
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

        // 1) 若消费带 items,按项目汇总金额并校验
        BigDecimal finalAmount = amount;
        List<TransactionItemRequest> effectiveItems = items == null ? Collections.emptyList() : items;
        List<MemberTransactionItem> persistItems = new ArrayList<>();
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

                MemberTransactionItem persist = new MemberTransactionItem();
                persist.setItemId(item.getId());
                persist.setItemName(item.getName());
                persist.setUnitPrice(unitPrice);
                persist.setQuantity(qty);
                persist.setSubtotal(subtotal);
                persist.setSortNo(i);
                persist.setCreatedTime(LocalDateTime.now());
                persistItems.add(persist);
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

        // 4) 写交易-项目明细(消费且 items 非空)
        if (!persistItems.isEmpty()) {
            for (MemberTransactionItem pi : persistItems) {
                pi.setTransactionId(transaction.getId());
            }
            for (MemberTransactionItem pi : persistItems) {
                transactionItemMapper.insert(pi);
            }
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
     * 读取某一笔交易的项目明细(用于消费接口返回 items 字段)
     */
    public List<TransactionItemResponse> listItemsByTransactionId(Long transactionId) {
        if (transactionId == null) {
            return Collections.emptyList();
        }
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
     * 分页查询交易流水,按 created_time DESC, id DESC 排序。
     * page 从 1 开始,size 默认 20,上限 100。
     */
    public MemberTransactionPageVO pageByMemberId(Long memberId, long page, long size) {
        long p = Math.max(page, 1);
        long s = Math.min(Math.max(size, 1), 100);
        Page<MemberTransaction> mpPage = new Page<>(p, s);
        LambdaQueryWrapper<MemberTransaction> wrapper = new LambdaQueryWrapper<MemberTransaction>()
                .eq(MemberTransaction::getMemberId, memberId)
                .orderByDesc(MemberTransaction::getCreatedTime)
                .orderByDesc(MemberTransaction::getId);
        IPage<MemberTransaction> result = transactionMapper.selectPage(mpPage, wrapper);
        List<MemberTransaction> records = result.getRecords();
        long total = result.getTotal();
        boolean hasMore = p * s < total;
        List<MemberTransactionResponse> responseList = toResponseListWithItems(records);
        return new MemberTransactionPageVO(responseList, total, p, s, hasMore);
    }

    /**
     * 将交易列表转换为带 items 的响应列表(批量查询明细,按 transactionId 分组)。
     */
    private List<MemberTransactionResponse> toResponseListWithItems(List<MemberTransaction> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> txIds = records.stream().map(MemberTransaction::getId).collect(Collectors.toList());
        Map<Long, List<TransactionItemResponse>> itemsByTxId = new HashMap<>();
        List<MemberTransactionItem> allItems = transactionItemMapper.selectList(
                new LambdaQueryWrapper<MemberTransactionItem>()
                        .in(MemberTransactionItem::getTransactionId, txIds)
                        .orderByAsc(MemberTransactionItem::getSortNo)
                        .orderByAsc(MemberTransactionItem::getId)
        );
        for (MemberTransactionItem mi : allItems) {
            itemsByTxId.computeIfAbsent(mi.getTransactionId(), k -> new ArrayList<>())
                    .add(toItemResponse(mi));
        }
        List<MemberTransactionResponse> responses = new ArrayList<>(records.size());
        for (MemberTransaction tx : records) {
            MemberTransactionResponse r = transactionConverter.toResponse(tx);
            r.setItems(itemsByTxId.getOrDefault(tx.getId(), Collections.emptyList()));
            responses.add(r);
        }
        return responses;
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
