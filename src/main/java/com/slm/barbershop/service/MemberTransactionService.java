package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.entity.MemberTransaction;
import com.slm.barbershop.enums.TransactionType;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.mapper.MemberTransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

    @Transactional
    public MemberTransaction store(Long memberId, BigDecimal amount, String remark) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
        }

        BigDecimal balanceBefore = member.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        member.setBalance(balanceAfter);
        memberMapper.updateById(member);

        MemberTransaction transaction = new MemberTransaction();
        transaction.setMemberId(memberId);
        transaction.setType(TransactionType.STORE.name());
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark(remark);
        transaction.setCreatedTime(LocalDateTime.now());
        transactionMapper.insert(transaction);

        return transaction;
    }

    @Transactional
    public MemberTransaction consume(Long memberId, BigDecimal amount, String remark) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
        }

        BigDecimal balanceBefore = member.getBalance();
        if (balanceBefore.compareTo(amount) < 0) {
            throw new BizException(HttpStatus.BAD_REQUEST, "余额不足");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        member.setBalance(balanceAfter);
        memberMapper.updateById(member);

        MemberTransaction transaction = new MemberTransaction();
        transaction.setMemberId(memberId);
        transaction.setType(TransactionType.CONSUME.name());
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark(remark);
        transaction.setCreatedTime(LocalDateTime.now());
        transactionMapper.insert(transaction);

        return transaction;
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

}