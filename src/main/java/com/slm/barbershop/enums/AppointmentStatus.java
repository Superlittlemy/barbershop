package com.slm.barbershop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约状态
 * <p>
 * 本期极简,无状态机,仅 0=有效 一个值,预留扩展。
 */
@Getter
@AllArgsConstructor
public enum AppointmentStatus {

    VALID(0, "有效");

    private final Integer code;
    private final String description;

    public static AppointmentStatus of(Integer code) {
        if (code == null) {
            return VALID;
        }
        for (AppointmentStatus s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        return VALID;
    }

}
