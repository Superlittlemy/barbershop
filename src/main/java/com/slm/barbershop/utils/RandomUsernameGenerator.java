package com.slm.barbershop.utils;

import java.security.SecureRandom;

public class RandomUsernameGenerator {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom random = new SecureRandom();

    // 示例输出：用户XZxJrbaI
    public static String generate() {
        StringBuilder sb = new StringBuilder("用户");
        for (int i = 0; i < 8; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

}
