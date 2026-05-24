package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slm.barbershop.entity.User;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.UserMapper;
import com.slm.barbershop.model.LoginRequest;
import com.slm.barbershop.model.LoginResponse;
import com.slm.barbershop.utils.JWTUtil;
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
            throw new BizException(HttpStatus.UNAUTHORIZED, "用户名不存在");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        String token = jwtUtil.generateJwtToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }

    public void register(String username, String password) {
        User existUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );

        if (existUser != null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
    }

    /**
     * 发送验证码
     */
    public void sendCode(String email) {
        // 先检查邮箱是否已注册
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        if (existUser != null && existUser.getEmailVerified()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "该邮箱已被注册");
        }

        // 生成并发送验证码
        String code = verifyCodeService.generateAndStore(email);
        emailService.sendVerifyCode(email, code);
    }

    /**
     * 邮箱注册/发送验证码（合并登录）
     * 如果邮箱已注册则只发送验证码，未注册则创建账号并发送验证码
     */
    public void registerByEmail(String email, String password) {
        // 检查邮箱是否已注册且已验证
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        if (existUser != null && existUser.getEmailVerified()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "该邮箱已被注册");
        }

        // 如果邮箱已注册但未验证，更新密码并发送验证码
        if (existUser != null) {
            existUser.setPassword(passwordEncoder.encode(password));
            userMapper.updateById(existUser);
        } else {
            // 新用户创建账号
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setEmailVerified(false);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);
        }

        // 发送验证码
        String code = verifyCodeService.generateAndStore(email);
        emailService.sendVerifyCode(email, code);
    }

    /**
     * 邮箱验证码登录
     */
    public LoginResponse emailLogin(String email, String code) {
        // 验证验证码
        if (!verifyCodeService.verify(email, code)) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "验证码错误或已过期");
        }

        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        if (user == null) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }

        // 更新邮箱验证状态
        user.setEmailVerified(true);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 生成token
        String token = jwtUtil.generateJwtToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }

}