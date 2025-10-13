package com.mogakjak.mogakjak.global.auth.service;

import com.mogakjak.mogakjak.global.auth.repository.RefreshTokenRedisRepository;
import com.mogakjak.mogakjak.global.auth.schema.RefreshToken;
import com.mogakjak.mogakjak.global.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static io.jsonwebtoken.lang.Assert.hasText;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenStorageService {

    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final JwtConfig jwtConfig;

    /**
     * session ID로 refresh token을 Redis에 저장 (다중 디바이스 지원)
     */
    public String storeRefreshToken(String userId, String refreshToken, String deviceInfo) {
        validateInput(userId, refreshToken);

        // JWT 설정의 refresh token 만료 시간을 '초' 단위로 사용 (Spring Data Redis @TimeToLive 기본 단위는 초)
        var exp = jwtConfig.getRefreshTokenExpiration();
        long ttlSeconds = exp.toSeconds();

        String sessionId = UUID.randomUUID().toString();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .id(sessionId)
                .token(refreshToken)
                .userId(userId)
                .sessionId(sessionId)
                .deviceInfo(deviceInfo != null ? deviceInfo : "알 수 없는 장치")
                .ttl(ttlSeconds)
                .build();

        refreshTokenRedisRepository.save(refreshTokenEntity);

        return sessionId;
    }

    /**
     * 입력값 검증
     */
    private void validateInput(String userId, String refreshToken) {
        hasText(userId, "userId는 빈 값일 수 없습니다.");
        hasText(refreshToken, "refreshToken은 빈 값일 수 없습니다.");
    }

    /**
     * 사용자의 모든 활성 세션 조회
     */
    public List<RefreshToken> getUserActiveSessions(String userId) {
        hasText(userId, "userId는 빈 값일 수 없습니다.");
        return refreshTokenRedisRepository.findByUserId(userId);
    }

    /**
     * 특정 세션 ID로 refresh token 삭제
     */
    public void removeSession(String sessionId) {
        hasText(sessionId, "sessionId는 빈 값일 수 없습니다.");
        refreshTokenRedisRepository.deleteById(sessionId);
        log.info("세션 삭제 완료: {}", sessionId);
    }
}
