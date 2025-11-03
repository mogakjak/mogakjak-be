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

    // todo Errors
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 카테고리를 찾을 수 없습니다."),
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 할 일을 찾을 수 없습니다."),
    FORBIDDEN_CATEGORY_ACCESS(HttpStatus.FORBIDDEN, "해당 카테고리에 접근 권한이 없습니다."),
    FORBIDDEN_TODO_ACCESS(HttpStatus.FORBIDDEN, "해당 할 일에 접근 권한이 없습니다."),

    // Timer Errors
    TIMER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 타이머 세션을 찾을 수 없습니다."),
    TIMER_ALREADY_RUNNING(HttpStatus.CONFLICT, "이미 진행 중인 타이머가 존재합니다."),
    TIMER_NOT_RUNNING(HttpStatus.BAD_REQUEST, "진행 중인 타이머가 없습니다."),
    TIMER_NOT_PAUSED(HttpStatus.BAD_REQUEST, "일시정지된 타이머가 없습니다."),
    INVALID_TIMER_MODE(HttpStatus.BAD_REQUEST, "잘못된 타이머 모드 요청입니다."),
    INVALID_POMODORO_SESSION(HttpStatus.BAD_REQUEST, "POMODORO 모드가 아닌 세션입니다."),
    POMODORO_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 모든 뽀모도로 라운드가 완료되었습니다."),
    POMODORO_INTERVAL_NOT_FOUND(HttpStatus.NOT_FOUND, "유효한 POMODORO 구간을 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
