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
    DEPRECATED_ENDPOINT(HttpStatus.GONE, "더 이상 사용되지 않는 엔드포인트입니다."),

    // todo Errors
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 카테고리를 찾을 수 없습니다."),
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 할 일을 찾을 수 없습니다."),
    FORBIDDEN_CATEGORY_ACCESS(HttpStatus.FORBIDDEN, "해당 카테고리에 접근 권한이 없습니다."),
    FORBIDDEN_TODO_ACCESS(HttpStatus.FORBIDDEN, "해당 할 일에 접근 권한이 없습니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 캐릭터를 찾을 수 없습니다."),
    CHARACTER_NOT_OWNED(HttpStatus.FORBIDDEN, "보유하지 않은 캐릭터입니다."),
    TODO_DELETED(HttpStatus.BAD_REQUEST, "해당 Todo는 논리적으로 삭제된 상태입니다."),

    // Timer Errors
    TIMER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 타이머 세션을 찾을 수 없습니다."),
    TIMER_ALREADY_RUNNING(HttpStatus.CONFLICT, "이미 진행 중인 타이머가 존재합니다."),
    TIMER_NOT_RUNNING(HttpStatus.BAD_REQUEST, "실행 중인 타이머가 없습니다."),
    NO_ACTIVE_TIMER_SESSION(HttpStatus.BAD_REQUEST, "실행 중이거나 일시정지된 타이머가 없습니다."),
    TIMER_NOT_PAUSED(HttpStatus.BAD_REQUEST, "일시정지된 타이머가 없습니다."),
    INVALID_TIMER_MODE(HttpStatus.BAD_REQUEST, "잘못된 타이머 모드 요청입니다."),
    INVALID_POMODORO_SESSION(HttpStatus.BAD_REQUEST, "POMODORO 모드가 아닌 세션입니다."),
    POMODORO_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 모든 뽀모도로 라운드가 완료되었습니다."),
    POMODORO_INTERVAL_NOT_FOUND(HttpStatus.NOT_FOUND, "유효한 POMODORO 구간을 찾을 수 없습니다."),
    FORBIDDEN_TIMER_ACCESS(HttpStatus.FORBIDDEN, "해당 타이머에 접근 권한이 없습니다."),
    INVALID_TARGET_TIME(HttpStatus.BAD_REQUEST, "유효하지 않은 todo의 목표 시간입니다. 목표 시간은 1분(60초)에서 24시간(86400초) 사이여야 합니다."),

    // Auth Errors
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // Group Errors
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다."),
    NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "그룹의 멤버가 아닙니다."),
    CANNOT_LEAVE_AS_HOST(HttpStatus.BAD_REQUEST, "방장은 다른 멤버가 있을 경우 그룹을 탈퇴할 수 없습니다."),
    ALREADY_GROUP_MEMBER(HttpStatus.CONFLICT, "이미 그룹에 속한 멤버입니다."),

    // Invitation Errors
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."),
    INVALID_INVITATION(HttpStatus.BAD_REQUEST, "유효하지 않은 초대입니다 (예: 만료, 이미 처리됨)."),
    CANNOT_INVITE_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 초대할 수 없습니다."),
    ALREADY_INVITED(HttpStatus.CONFLICT, "이미 초대를 보낸 사용자입니다."),

    // Quote Errors
    QUOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "명언을 찾을 수 없습니다."),

    // User Errors
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
  
    // Active Focus Session Errors
    ACTIVE_SESSION_EXISTS(HttpStatus.CONFLICT, "해당 계정에 이미 실행 중인 타이머가 있습니다."),
    ACTIVE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 계정에 활성화된 세션이 존재하지 않습니다."),
    FORBIDDEN_ACTIVE_SESSION(HttpStatus.FORBIDDEN, "해당 활성 세션에 접근 권한이 없습니다."),

    // Focus Session Errors
    FORBIDDEN_SESSION(HttpStatus.FORBIDDEN, "해당 집중 세션에 접근 권한이 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "집중 세션을 찾을 수 없습니다."),
    SESSION_ALREADY_RUNNING(HttpStatus.BAD_REQUEST, "세션이 실행 중인 상태입니다."),
    SESSION_ALREADY_PAUSED(HttpStatus.BAD_REQUEST, "세션은 정지된 상태입니다."),
    SESSION_ALREADY_FINISHED(HttpStatus.BAD_REQUEST, "세션은 종료된 상태입니다."),
    SESSION_NOT_RUNNING(HttpStatus.BAD_REQUEST, "세션이 실행 중이지 않습니다."),
    SESSION_NOT_PAUSED(HttpStatus.BAD_REQUEST, "세션이 정지 상태이지 않습니다."),
    SESSION_NOT_FINISHABLE(HttpStatus.BAD_REQUEST, "세션을 종료할 수 없습니다."),

    // Focus Interval Errors
    INTERVAL_NOT_FOUND(HttpStatus.NOT_FOUND, "집중 구간(Interval)을 찾을 수 없습니다."),
    INVALID_POMODORO_PHASE_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 뽀모도로 Interval Type입니다. FOCUS / BREAK 만 허용됩니다."),
    PHASE_NOT_FINISHED(HttpStatus.BAD_REQUEST, "현재의 뽀모도로 단계가 아직 종료되지 않았습니다."),

    // Feedback Errors
    INVALID_FEEDBACK_SCORE(HttpStatus.BAD_REQUEST, "유효하지 않은 피드백 점수입니다. 1-5 사이의 자연수만 가능합니다."),
    FEEDBACK_TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "피드백 태그를 찾을 수 없습니다."),
    DUPLICATE_FEEDBACK_TAG(HttpStatus.CONFLICT, "이미 존재하는 태그 코드입니다."),
    INVALID_FEEDBACK_TAG_TYPE(HttpStatus.BAD_REQUEST, "선택한 점수에서는 해당 태그를 사용할 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
