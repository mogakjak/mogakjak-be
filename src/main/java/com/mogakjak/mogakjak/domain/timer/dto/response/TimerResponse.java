package com.mogakjak.mogakjak.domain.timer.dto.response;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.todo.controller.dto.SimpleTodoResponse;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record TimerResponse(

        @Schema(description = "타이머 세션 ID")
        UUID sessionId,

        @Schema(description = "타이머 모드", example = "TIMER | STOPWATCH | POMODORO")
        TimerMode mode,

        @Schema(description = "세션 상태", example = "FINISHED")
        TimerStatus status,

        @Schema(description = "시작 시각", example = "")
        LocalDateTime startedAt,

        @Schema(description = "중지 시각")
        LocalDateTime pausedAt,

        @Schema(description = "종료 시각")
        LocalDateTime endedAt,

        @Schema(description = "목표 시간(초 단위)", example = "")
        Long targetDuration,

        @Schema(description = "총 집중 시간(초 단위)", example = "")
        Long totalDuration,

        @Schema(description = "달성률(0~100)", example = "0")
        Integer progressRate,

        @Schema(description = "할 일(todo) 관련 dto")
        SimpleTodoResponse todo,

        @Schema(description = "뽀모도로 관련 dto")
        PomodoroInfoResponse pomodoroInfo

) {
    public static TimerResponse fromStart(FocusSession focusSession, Todo todo) {
        return new TimerResponse(
                focusSession.getId(),
                focusSession.getMode(),
                focusSession.getStatus(),
                focusSession.getStartedAt(),
                null,
                null,
                focusSession.getTargetDuration(),
                focusSession.getTotalDuration(),
                focusSession.getProgressRate(),
                SimpleTodoResponse.from(todo),
                null
        );
    }

    public static TimerResponse fromPause(FocusSession focusSession, LocalDateTime pausedAt) {
        return new TimerResponse(
                focusSession.getId(),
                focusSession.getMode(),
                focusSession.getStatus(),
                focusSession.getStartedAt(),
                pausedAt,
                null,
                focusSession.getTargetDuration(),
                focusSession.getTotalDuration(),
                focusSession.getProgressRate(),
                null,
                null
        );
    }

    public static TimerResponse fromResume(FocusSession focusSession) {
        return new TimerResponse(
                focusSession.getId(),
                focusSession.getMode(),
                focusSession.getStatus(),
                focusSession.getStartedAt(),
                null,
                null,
                focusSession.getTargetDuration(),
                focusSession.getTotalDuration(),
                focusSession.getProgressRate(),
                null,
                null
        );
    }

    public static TimerResponse fromFinish(FocusSession focusSession) {
        return new TimerResponse(
                focusSession.getId(),
                focusSession.getMode(),
                focusSession.getStatus(),
                focusSession.getStartedAt(),
                null,
                focusSession.getEndedAt(),
                focusSession.getTargetDuration(),
                focusSession.getTotalDuration(),
                focusSession.getProgressRate(),
                null,
                null
        );
    }

    public static TimerResponse fromPomodoroPhaseChange(FocusSession focusSession, FocusInterval phaseInterval) {
        return new TimerResponse(
                focusSession.getId(),
                focusSession.getMode(),
                focusSession.getStatus(),
                focusSession.getStartedAt(),
                null,
                focusSession.getEndedAt(),
                focusSession.getTargetDuration(),
                focusSession.getTotalDuration(),
                focusSession.getProgressRate(),
                null,
                PomodoroInfoResponse.from(focusSession, phaseInterval)
        );
    }
}
