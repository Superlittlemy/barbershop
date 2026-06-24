package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.MemberTransactionConverter;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.entity.MemberTransaction;
import com.slm.barbershop.enums.TransactionType;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.lock.DistributedLock;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.mapper.MemberTransactionMapper;
import com.slm.barbershop.model.MemberTransactionPageVO;
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
import java.util.List;

@Service
public class MemberTransactionService extends ServiceImpl<MemberTransactionMapper, MemberTransaction> {

    @Autowired
    private MemberTransactionMapper transactionMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberTransactionConverter transactionConverter;

    /**
     * 注入自身代理,使 {@link #doUpdate} 上的 @Transactional 通过代理生效
     */
    @Autowired
    @Lazy
    private MemberTransactionService self;

    public MemberTransaction store(Long memberId, BigDecimal amount, String remark, String idempotencyKey) {
        return applyBalance(memberId, amount, TransactionType.STORE, remark, idempotencyKey);
    }

    public MemberTransaction consume(Long memberId, BigDecimal amount, String remark, String idempotencyKey) {
        return applyBalance(memberId, amount, TransactionType.CONSUME, remark, idempotencyKey);
    }

    /**
     * 公共余额变更流程:
     * 1. 幂等前置查询(命中 → 直接返回原流水)
     * 2. 走锁策略,在事务内重试:读 member → 计算余额 → updateById(CAS) → 写流水
     */
    private MemberTransaction applyBalance(Long memberId,
                                           BigDecimal amount,
                                           TransactionType type,
                                           String remark,
                                           String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            MemberTransaction existed = findByIdempotencyKey(idempotencyKey);
            if (existed != null) {
                return existed;
            }
        }

        return self.doUpdate(memberId, amount, type, remark, idempotencyKey);
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
                                      String idempotencyKey) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
        }

        BigDecimal balanceBefore = member.getBalance();
        BigDecimal balanceAfter;
        if (type == TransactionType.CONSUME) {
            if (balanceBefore.compareTo(amount) < 0) {
                throw new BizException(HttpStatus.BAD_REQUEST, "余额不足");
            }
            balanceAfter = balanceBefore.subtract(amount);
        } else {
            balanceAfter = balanceBefore.add(amount);
        }

        member.setBalance(balanceAfter);
        int rows = memberMapper.updateById(member);
        if (rows == 0) {
            // @Version 触发 CAS 失败,抛出让策略重试
            throw new OptimisticLockingFailureException(
                    "member balance update conflict, memberId=" + memberId);
        }

        MemberTransaction transaction = new MemberTransaction();
        transaction.setMemberId(memberId);
        transaction.setType(type.name());
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark(remark);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setCreatedTime(LocalDateTime.now());
        try {
            transactionMapper.insert(transaction);
        } catch (DuplicateKeyException e) {
            // 并发同 idempotencyKey,事务回滚,余额一并撤销
            throw new BizException(HttpStatus.CONFLICT, "重复提交");
        }

        return transaction;
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

    public List<MemberTransaction> listByMemberId(Long memberId) {
        return transactionMapper.selectList(
                new LambdaQueryWrapper<MemberTransaction>()
                        .eq(MemberTransaction::getMemberId, memberId)
                        .orderByDesc(MemberTransaction::getCreatedTime)
        );
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
        return new MemberTransactionPageVO(
                transactionConverter.toResponseList(records),
                total, p, s, hasMore);
    }

}