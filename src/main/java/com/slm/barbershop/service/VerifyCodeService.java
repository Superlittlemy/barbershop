package com.slm.barbershop.service;

import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class VerifyCodeService {

    private static final int CODE_VALID_DURATION = 15 * 60 * 1000; // 15分钟
    private static final int MAX_ATTEMPTS = 5;
    private static final int CODE_LENGTH = 6;

    private final ConcurrentHashMap<String, VerifyCode> verifyCodes = new ConcurrentHashMap<>();

    @Data
    public static class VerifyCode {
        private String code;
        private long expireTime;
        private int attemptCount;
    }

    /**
     * 生成6位验证码并存储
     */
    public String generateAndStore(String email) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        VerifyCode verifyCode = new VerifyCode();
        verifyCode.setCode(code);
        verifyCode.setExpireTime(System.currentTimeMillis() + CODE_VALID_DURATION);
        verifyCode.setAttemptCount(0);
        verifyCodes.put(email, verifyCode);
        return code;
    }

    /**
     * 验证验证码
     * @return true 验证成功, false 验证失败
     */
    public boolean verify(String email, String code) {
        VerifyCode verifyCode = verifyCodes.get(email);
        if (verifyCode == null) {
            return false;
        }
        if (System.currentTimeMillis() > verifyCode.getExpireTime()) {
            verifyCodes.remove(email);
            return false;
        }
        if (verifyCode.getAttemptCount() >= MAX_ATTEMPTS) {
            verifyCodes.remove(email);
            return false;
        }
        if (verifyCode.getCode().equals(code)) {
            verifyCodes.remove(email);
            return true;
        }
        verifyCode.setAttemptCount(verifyCode.getAttemptCount() + 1);
        return false;
    }

    /**
     * 获取验证码（用于测试或调试，不暴露给外部）
     */
    public String getCode(String email) {
        VerifyCode verifyCode = verifyCodes.get(email);
        return verifyCode != null ? verifyCode.getCode() : null;
    }

    /**
     * 删除验证码
     */
    public void remove(String email) {
        verifyCodes.remove(email);
    }
}