package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slm.barbershop.entity.User;
import com.slm.common.enums.ResultStatus;
import com.slm.common.exception.BizException;
import com.slm.barbershop.mapper.UserMapper;
import com.slm.barbershop.model.LoginRequest;
import com.slm.barbershop.model.LoginResponse;
import com.slm.barbershop.utils.JWTUtil;
import com.slm.common.utils.RandomUsernameGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private VerifyCodeService verifyCodeService;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );

        if (user == null) {
            throw new BizException(ResultStatus.USER_OR_PASSWORD_WRONG, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ResultStatus.USER_OR_PASSWORD_WRONG, "用户名或密码错误");
        }

        String token = jwtUtil.generateJwtToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }

    public void register(String username, String password) {
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );

        if (existUser != null) {
            throw new BizException(ResultStatus.BAD_REQUEST, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.insert(user);
    }

    /**
     * 发送验证码到邮箱
     */
    public void sendCode(String email) {
        String code = verifyCodeService.generateAndStore(email);
        emailService.sendVerifyCode(email, code);
    }

    /**
     * 邮箱验证码登录（合并注册）
     * 验证码通过后：
     * - 已注册用户直接登录
     * - 未注册用户自动注册并登录
     */
    public LoginResponse emailLogin(String email, String code) {
        // 验证验证码
        if (!verifyCodeService.verify(email, code)) {
            throw new BizException(ResultStatus.INVALID_VERIFY_CODE, "验证码错误或已过期");
        }

        // 查询或创建用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        if (user == null) {
            // 未注册用户，自动创建账号
            user = new User();
            user.setUsername(RandomUsernameGenerator.generate());
            user.setEmail(email);
            user.setCreatedTime(LocalDateTime.now());
            user.setUpdatedTime(LocalDateTime.now());
            userMapper.insert(user);
        }

        // 生成token
        String token = jwtUtil.generateJwtToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }

}