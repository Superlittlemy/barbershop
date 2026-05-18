package com.slm.barbershop.utils;

public class MaskUtil {

    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 11) {
            return mobile; // 存储需严格校验手机号长度
        }
        return mobile.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

}
