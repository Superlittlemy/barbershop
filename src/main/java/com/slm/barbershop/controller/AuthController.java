package com.slm.barbershop.controller;

import com.slm.barbershop.model.LoginRequest;
import com.slm.barbershop.model.LoginResponse;
import com.slm.barbershop.service.AuthService;
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
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public void register(@RequestBody LoginRequest request) {
        authService.register(request.getUsername(), request.getPassword());
    }

}