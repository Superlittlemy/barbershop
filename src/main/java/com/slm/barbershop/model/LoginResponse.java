package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "JWT令牌")
    private String token;

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

}