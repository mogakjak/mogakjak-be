package com.mogakjak.mogakjak.global.auth.controller;

import com.mogakjak.mogakjak.global.auth.dto.LoginResponse;
import com.mogakjak.mogakjak.global.auth.service.AuthService;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController{

    private final AuthService authService;

    @PostMapping("/auth/refresh")
    public ApiResponse<LoginResponse> refreshToken(
            @RequestHeader(value = "Refresh-Token", required = false) String refreshTokenHeader) {
        return authService.handleRefreshToken(refreshTokenHeader);
    }

}
