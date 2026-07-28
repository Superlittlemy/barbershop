package com.slm.barbershop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账单类型
 */
@Getter
@AllArgsConstructor
public enum BillType {

    CONSUME("消费"),
    STORE("储值");

    private final String description;

    public static BillType of(String name) {
        if (name == null) {
            return null;
        }
        for (BillType t : values()) {
            if (t.name().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

}