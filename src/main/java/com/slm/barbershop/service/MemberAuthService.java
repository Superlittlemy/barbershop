package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.MemberMapper;
import com.slm.barbershop.model.LoginResponse;
import com.slm.barbershop.utils.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MemberAuthService {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private JWTUtil jwtUtil;

    /**
     * 会员登录：phone + name + shopId 三项校验,签发 JWT
     */
    public LoginResponse login(String phone, String name, Long shopId) {
        if (phone == null || phone.isEmpty()
                || name == null || name.isEmpty()
                || shopId == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "参数不完整");
        }

        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getShopId, shopId)
                        .eq(Member::getPhone, phone.trim())
                        .eq(Member::getName, name.trim())
                        .eq(Member::getIsDeleted, 0)
                        .last("LIMIT 1")
        );

        if (member == null) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "姓名或手机号不正确");
        }

        String subject = JWTUtil.MEMBER_TOKEN_SUBJECT_PREFIX + member.getId();
        String token = jwtUtil.generateJwtToken(member.getId(), member.getName(), subject);
        return new LoginResponse(token, member.getId(), member.getName());
    }

}