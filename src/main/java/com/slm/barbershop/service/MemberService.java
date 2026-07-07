package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.MemberConverter;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.model.MemberMatchVO;
import com.slm.barbershop.model.MemberRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService extends ServiceImpl<MemberMapper, Member> {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberConverter memberConverter;

    public Member create(Long shopId, MemberRequest request) {
        assertPhoneUnique(shopId, request.getPhone(), null);
        Member member = memberConverter.toEntityWithShopId(request, shopId);
        member.setBalance(BigDecimal.ZERO);
        memberMapper.insert(member);
        return member;
    }

    public Member update(Long shopId, Long id, MemberRequest request) {
        Member member = this.getByIdAndShopId(shopId, id).orElseThrow(() -> new BizException(HttpStatus.NOT_FOUND, "会员不存在"));
        assertPhoneUnique(shopId, request.getPhone(), id);
        // 只更新 name / phone，余额由 MemberTransactionService 单独维护
        member.setName(request.getName());
        member.setPhone(request.getPhone());
        memberMapper.updateById(member);
        return member;
    }

    /**
     * 校验同店铺下手机号唯一。
     * excludeMemberId 非空时排除自身(用于编辑场景),空时查全表(用于新增场景)。
     * phone 为空时直接放行——必填校验由 MemberRequest.@NotNull 兜底。
     */
    private void assertPhoneUnique(Long shopId, String phone, Long excludeMemberId) {
        if (phone == null || phone.isEmpty()) return;
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<Member>()
                .eq(Member::getShopId, shopId)
                .eq(Member::getPhone, phone);
        if (excludeMemberId != null) {
            wrapper.ne(Member::getId, excludeMemberId);
        }
        if (memberMapper.selectCount(wrapper) > 0) {
            throw new BizException(HttpStatus.BAD_REQUEST, "该店铺已存在相同手机号的会员");
        }
    }

    public void delete(Long id) {
        this.removeById(id);
    }

    public List<Member> listByShopId(Long shopId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getShopId, shopId)
        );
    }

    public IPage<Member> page(IPage<Member> page, Long shopId) {
        return memberMapper.selectPage(page, new LambdaQueryWrapper<Member>().eq(Member::getShopId, shopId));
    }

    /**
     * 跨店铺查找匹配手机号+姓名的会员。
     * 入参校验由 Controller 完成；这里只负责查询。
     */
    public List<MemberMatchVO> match(String phone, String name) {
        if (phone == null || phone.isEmpty() || name == null || name.isEmpty()) {
            return Collections.emptyList();
        }
        return memberMapper.matchAcrossShops(phone.trim(), name.trim());
    }

    public Optional<Member> getByIdAndShopId(Long shopId, Long id) {
        return this.lambdaQuery().eq(Member::getShopId, shopId).eq(Member::getId, id).oneOpt();
    }

}
