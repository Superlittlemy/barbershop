package com.slm.barbershop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消费项目状态
 */
@Getter
@AllArgsConstructor
public enum ServiceItemStatus {

    OFF(0, "下架"),
    ON(1, "上架");

    private final Integer code;
    private final String description;

    public static ServiceItemStatus of(Integer code) {
        if (code == null) {
            return ON;
        }
        for (ServiceItemStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return ON;
    }

}
