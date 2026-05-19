package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员响应
 */
@Data
@Schema(description = "会员响应")
public class MemberResponse {

    @Schema(description = "会员ID")
    private Long id;

    @Schema(description = "所属店铺ID")
    private Long shopId;

    @Schema(description = "会员姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "余额")
    private BigDecimal balance;

}