package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.MemberConverter;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.model.MemberRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MemberService extends ServiceImpl<MemberMapper, Member> {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberConverter memberConverter;

    public Member create(Long shopId, MemberRequest request) {
        Member member = memberConverter.toEntityWithShopId(request, shopId);
        member.setBalance(BigDecimal.ZERO);
        memberMapper.insert(member);
        return member;
    }

    public Member update(Long shopId, Long id, MemberRequest request) {
        Member member = getByIdAndShopId(shopId, id);
        if (member == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        Member updated = memberConverter.toEntity(request);
        updated.setId(id);
        updated.setShopId(shopId);
        updated.setBalance(member.getBalance());
        memberMapper.updateById(updated);
        return member;
    }

    public void delete(Long shopId, Long id) {
        Member member = getByIdAndShopId(shopId, id);
        if (member == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "会员不存在");
        }
        memberMapper.deleteById(id);
    }

    public Member getById(Long shopId, Long id) {
        return getByIdAndShopId(shopId, id);
    }

    public List<Member> listByShopId(Long shopId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getShopId, shopId)
        );
    }

    private Member getByIdAndShopId(Long shopId, Long id) {
        return memberMapper.selectOne(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getShopId, shopId)
                        .eq(Member::getId, id)
        );
    }

}