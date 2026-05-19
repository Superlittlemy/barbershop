package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会员请求
 */
@Data
@Schema(description = "会员请求")
public class MemberRequest {

    @Schema(description = "所属店铺ID")
    private Long shopId;

    @Schema(description = "会员姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

}