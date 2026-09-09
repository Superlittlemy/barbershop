package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slm.barbershop.entity.Member;
import com.slm.common.enums.ResultStatus;
import com.slm.common.exception.BizException;
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
     * 会员登录：phone + shopId 两项校验,签发 JWT
     */
    public LoginResponse login(String phone, Long shopId) {
        if (phone == null || phone.isEmpty()
                || shopId == null) {
            throw new BizException(ResultStatus.BAD_REQUEST, "参数不完整");
        }

        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getShopId, shopId)
                        .eq(Member::getPhone, phone.trim())
                        .eq(Member::getIsDeleted, 0)
                        .last("LIMIT 1")
        );

        if (member == null) {
            throw new BizException(ResultStatus.USER_OR_PASSWORD_WRONG, "非注册会员");
        }

        String subject = JWTUtil.MEMBER_TOKEN_SUBJECT_PREFIX + member.getId();
        String token = jwtUtil.generateJwtToken(member.getId(), member.getName(), subject);
        return new LoginResponse(token, member.getId(), member.getName());
    }

}