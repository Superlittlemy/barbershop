package com.slm.barbershop.controller;

import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.AuthUser;
import com.slm.barbershop.model.LoginRequest;
import com.slm.barbershop.model.LoginResponse;
import com.slm.barbershop.service.AuthService;
import com.slm.barbershop.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证", description = "用户认证相关接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody LoginRequest request) {
        authService.register(request.getUsername(), request.getPassword());
        return ApiResponse.ok();
    }

    @Operation(summary = "获取当前用户")
    @GetMapping("/me")
    public ApiResponse<AuthUser> getCurrentUser() {
        return ApiResponse.ok(UserContext.getUser());
    }

}