package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 会员请求
 */
@Data
@Schema(description = "会员请求")
public class MemberRequest {

    @NotNull(message = "所属店铺ID不能为空")
    @Schema(description = "所属店铺ID")
    private Long shopId;

    @Schema(description = "会员姓名")
    private String name;

    @NotNull(message = "手机号不能为空")
    @Schema(description = "手机号")
    private String phone;

}