package com.mogakjak.mogakjak.domain.timer.dto.response;

import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
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
        Integer progressRate

) {
    public static TimerResponse fromStartAndResume(FocusSession focusSession) {
        return new TimerResponse(
                focusSession.getId(),
                focusSession.getMode(),
                focusSession.getStatus(),
                focusSession.getStartedAt(),
                null,
                null,
                focusSession.getTargetDuration(),
                focusSession.getTotalDuration(),
                focusSession.getProgressRate()
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
                focusSession.getProgressRate()
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
                focusSession.getProgressRate()
        );
    }
}
