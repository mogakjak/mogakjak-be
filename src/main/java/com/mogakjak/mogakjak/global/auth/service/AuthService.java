package com.mogakjak.mogakjak.global.auth.service;

import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserProvider;
import com.mogakjak.mogakjak.global.enumerate.ProviderType;
import com.mogakjak.mogakjak.domain.user.repository.UserProviderRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.domain.user.service.UserService;
import com.mogakjak.mogakjak.global.auth.dto.LoginResponse;
import com.mogakjak.mogakjak.global.auth.dto.TokenRefreshResponse;
import com.mogakjak.mogakjak.global.auth.exception.AuthExceptionCode;
import com.mogakjak.mogakjak.global.auth.security.util.JwtUtil;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.config.JwtConfig;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;
    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final TokenStorageService tokenStorageService;

    public ApiResponse<TokenRefreshResponse> handleRefreshToken(String refreshTokenHeader) {
        if (refreshTokenHeader == null || refreshTokenHeader.isBlank()) {
            throw new CustomException(AuthExceptionCode.TOKEN_NOT_FOUND);
        }

        TokenRefreshResponse tokenResponse = this.refreshToken(refreshTokenHeader);

        return ApiResponse.success(SuccessCode.OK, tokenResponse);
    }


    @Transactional
    public LoginResponse oauth2Login(String email, String name, ProviderType providerType, Object providerId) {
        // 입력 검증 및 이메일 정규화
        if (email == null || email.isBlank() || providerType == null) {
            throw new CustomException(AuthExceptionCode.OAUTH2_FAILURE);
        }
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);

        // 사용자 조회 또는 생성
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> userService.createUser(normalizedEmail, name));

        // 탈퇴된 유저인 경우 에러 반환
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new CustomException(ErrorCode.WITHDRAWN_USER);
        }

        // OAuth2 제공자 정보를 user_provider 테이블에 저장 (보안 검증 포함)
        if (providerId == null) {
            throw new CustomException(AuthExceptionCode.OAUTH2_FAILURE);
        }
        String providerIdStr = providerId.toString();
        Optional<UserProvider> upOpt = userProviderRepository.findByProviderIdAndType(providerIdStr, providerType);
        UserProvider userProvider = upOpt
                .map(up -> {
                    if (!up.getUser().getId().equals(user.getId())) {
                        throw new CustomException(AuthExceptionCode.AUTHENTICATION_FAILED); // 교차 링크 차단
                    }
                    return up;
                })
                .orElseGet(() -> {
                    UserProvider newProvider = userService.createUserProvider(user, providerType, providerIdStr);
                    return newProvider;
                });

        return LoginResponse.builder()
                .email(user.getEmail())
                .name(user.getName())
                .userId(user.getId().toString())
                .expiresIn((int) jwtConfig.getAccessTokenExpiration().toSeconds())
                .build();
    }

    /**
     * Refresh Token으로 새로운 Access Token과 Refresh Token 발급 (토큰 회전)
     */
    public TokenRefreshResponse refreshToken(String refreshToken) {
        // 1. refresh token 타입 검증 (typ=refresh 강제)
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new CustomException(AuthExceptionCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtUtil.getEmailFromToken(refreshToken);
        String userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(AuthExceptionCode.USER_NOT_FOUND));

        // 2. 세션 바인딩: Redis에 저장된 refresh token과 대조
        boolean matched = tokenStorageService.getUserActiveSessions(userId)
                .stream().anyMatch(rt -> refreshToken.equals(rt.token()));
        if (!matched) {
            log.warn("Redis에 저장되지 않은 refresh token 사용 시도: userId={}, email={}", userId, email);
            throw new CustomException(AuthExceptionCode.INVALID_REFRESH_TOKEN);
        }

        // 3. 토큰 회전: 새로운 access token과 refresh token 생성
        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getName(), user.getId().toString());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getName(), user.getId().toString());

        // 4. 기존 refresh token 무효화 (Redis에서 제거)
        tokenStorageService.getUserActiveSessions(userId)
                .stream()
                .filter(rt -> refreshToken.equals(rt.token()))
                .findFirst()
                .ifPresent(rt -> tokenStorageService.removeSession(rt.sessionId()));

        // 5. 새로운 refresh token을 Redis에 저장
        String sessionId = tokenStorageService.storeRefreshToken(userId, newRefreshToken, "Token Refresh");

        log.info("토큰 회전 성공: {} (기존 토큰 무효화, 새 토큰 발급)", user.getEmail());

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .sessionId(sessionId)
                .userInfo(LoginResponse.builder()
                        .email(user.getEmail())
                        .name(user.getName())
                        .userId(user.getId().toString())
                        .expiresIn((int) jwtConfig.getAccessTokenExpiration().toSeconds())
                        .build())
                .build();
    }
}
