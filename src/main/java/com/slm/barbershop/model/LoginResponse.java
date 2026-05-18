package com.slm.barbershop.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private Long id;
    private String username;

}