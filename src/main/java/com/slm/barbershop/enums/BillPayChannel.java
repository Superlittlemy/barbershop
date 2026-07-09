package com.slm.barbershop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账单支付方式
 */
@Getter
@AllArgsConstructor
public enum BillPayChannel {

    MEMBER("会员划账"),
    OFFLINE("线下"),
    WECHAT("微信"),
    ALIPAY("支付宝");

    private final String description;

    public static BillPayChannel of(String name) {
        if (name == null) {
            return null;
        }
        for (BillPayChannel c : values()) {
            if (c.name().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }

}
