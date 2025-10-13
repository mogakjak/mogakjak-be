package com.mogakjak.mogakjak.global.auth.security.util;

import com.mogakjak.mogakjak.global.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    public String generateAccessToken(String email, String name, String userId) {
        return generateToken(email, name, userId, jwtConfig.getAccessTokenExpiration(), "access");
    }

    public String generateRefreshToken(String email, String name, String userId) {
        return generateToken(email, name, userId, jwtConfig.getRefreshTokenExpiration(), "refresh");
    }

    private String generateToken(String email, String name, String userId, Duration expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration.toMillis());

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("userId", userId);
        claims.put("name", name);
        claims.put("typ", tokenType);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuer(jwtConfig.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT 토큰 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("typ", String.class);
            if (!"access".equals(tokenType)) {
                log.warn("Access 토큰이 아닌 토큰 사용 시도: {}", tokenType);
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Access 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("typ", String.class);
            if (!"refresh".equals(tokenType)) {
                log.warn("Refresh 토큰이 아닌 토큰 사용 시도: {}", tokenType);
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Refresh 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("userId", String.class) : null;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }
}

