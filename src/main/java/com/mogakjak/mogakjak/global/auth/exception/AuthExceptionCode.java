package com.mogakjak.mogakjak.global.auth.exception;

import com.mogakjak.mogakjak.global.exception.status.StatusCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthExceptionCode implements StatusCode {

    OAUTH2_FAILURE(HttpStatus.BAD_REQUEST, "OAuth2 인증에 실패했습니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰을 찾을 수 없습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
    EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "OAuth2 제공자로부터 이메일 정보를 가져올 수 없습니다. 이메일 동의가 필요합니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Refresh token을 찾을 수 없습니다."),
    PROVIDER_ID_NOT_FOUND(HttpStatus.BAD_REQUEST, "제공자 ID를 찾을 수 없습니다."),
    PROVIDER_TYPE_NOT_FOUND(HttpStatus.BAD_REQUEST, "제공자 타입을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh token입니다."),
    OAUTH2_LOGIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAuth2 로그인 처리 중 오류가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 유저입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}

