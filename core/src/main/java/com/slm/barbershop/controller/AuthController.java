package com.slm.barbershop.controller;

import com.slm.common.model.ApiResponse;
import com.slm.common.model.AuthUser;
import com.slm.barbershop.model.EmailCodeRequest;
import com.slm.barbershop.model.LoginRequest;
import com.slm.barbershop.model.LoginResponse;
import com.slm.barbershop.service.AuthService;
import com.slm.common.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@Tag(name = "认证", description = "用户认证相关接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody LoginRequest request) {
        authService.register(request.getUsername(), request.getPassword());
        return ApiResponse.success();
    }

    @Operation(summary = "获取当前用户")
    @GetMapping("/me")
    public ApiResponse<AuthUser> getCurrentUser() {
        return ApiResponse.success(UserContext.getUser());
    }

    @Operation(summary = "发送验证码")
    @PostMapping("/send-email-code")
    public ApiResponse<Void> sendCode(@RequestParam String email) {
        authService.sendCode(email);
        return ApiResponse.success();
    }

    @Operation(summary = "邮箱验证码登录（支持自动注册）")
    @PostMapping("/login/email")
    public ApiResponse<LoginResponse> loginByEmail(@RequestBody @Valid EmailCodeRequest request) {
        return ApiResponse.success(authService.emailLogin(request.getEmail(), request.getCode()));
    }

}