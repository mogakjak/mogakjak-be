package com.mogakjak.mogakjak.global.exception.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements StatusCode {

    // Common Errors
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // todo Erros
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 카테고리를 찾을 수 없습니다."),
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 할 일을 찾을 수 없습니다."),
    FORBIDDEN_CATEGORY_ACCESS(HttpStatus.FORBIDDEN, "해당 카테고리에 접근 권한이 없습니다."),
    FORBIDDEN_TODO_ACCESS(HttpStatus.FORBIDDEN, "해당 할 일에 접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
