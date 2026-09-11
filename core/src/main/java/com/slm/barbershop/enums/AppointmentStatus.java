package com.slm.barbershop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预约状态
 * <p>
 * 0=有效(创建初始态,或取消后又重新创建)
 * 1=已取消(会员自助取消后的终态;过期历史预约在查询时另行按 endTime>now 过滤)
 */
@Getter
@AllArgsConstructor
public enum AppointmentStatus {

    VALID(0, "有效"),
    CANCELLED(1, "已取消");

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
