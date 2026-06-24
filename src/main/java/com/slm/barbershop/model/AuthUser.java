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

    public static final String TYPE_USER = "user";
    public static final String TYPE_MEMBER = "member";

    private Long id;
    private String username;
    private String type;

    public AuthUser(Long id, String username) {
        this(id, username, TYPE_USER);
    }

    public boolean isMember() {
        return TYPE_MEMBER.equals(type);
    }

}