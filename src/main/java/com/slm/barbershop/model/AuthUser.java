package com.slm.barbershop.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {

    private Long id;
    private String username;

}