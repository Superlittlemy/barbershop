package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会员登录请求
 */
@Data
@Schema(description = "会员登录请求")
public class MemberLoginRequest {

    @Schema(description = "手机号", required = true, example = "13800008888")
    private String phone;

    @Schema(description = "店铺ID", required = true, example = "1")
    private Long shopId;

}
