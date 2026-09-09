package com.slm.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证用户信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "认证用户信息")
public class AuthUser {

    public static final String TYPE_USER = "user";
    public static final String TYPE_MEMBER = "member";

    @Schema(description = "用户id")
    private Long id;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "用户类型")
    private String type;

    public AuthUser(Long id, String username) {
        this(id, username, TYPE_USER);
    }

    public boolean isMember() {
        return TYPE_MEMBER.equals(type);
    }

}