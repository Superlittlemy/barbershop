package com.slm.common.utils;

import java.util.concurrent.ThreadLocalRandom;

public class TraceIdGenerator {

    private static final char[] CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int LENGTH = 8;

    public static String generate() {
        char[] buf = new char[LENGTH];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < LENGTH; i++) {
            buf[i] = CHARS[random.nextInt(CHARS.length)];
        }
        return new String(buf);
    }

}