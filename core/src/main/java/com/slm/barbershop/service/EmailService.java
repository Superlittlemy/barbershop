package com.slm.barbershop.service;

import com.slm.common.enums.ResultStatus;
import com.slm.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * 发送验证码邮件
     * @param to 收件人邮箱
     * @param code 验证码
     */
    public void sendVerifyCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Barbershop验证码");
            message.setText("您的验证码是：" + code + "\n验证码有效期15分钟，请勿泄露给他人。");
            javaMailSender.send(message);
            log.info("邮箱验证码已发送到: {}", to);
        } catch (Exception e) {
            log.error("发送邮箱验证码失败: {}", to, e);
            throw new BizException(ResultStatus.UNAUTHORIZED, "发送邮箱验证码失败");
        }
    }

}