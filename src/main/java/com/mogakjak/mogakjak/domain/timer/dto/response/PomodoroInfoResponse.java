package com.mogakjak.mogakjak.domain.timer.dto.response;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record PomodoroInfoResponse (

        @Schema(description = "목표 집중 시간(초 단위)", example = "1800")
        Long focusDuration,

        @Schema(description = "목표 휴식 시간(초 단위)", example = "600")
        Long breakDuration,

        @Schema(description = "목표 반복 횟수", example = "2")
        Integer repeatCount,

        @Schema(description = "뽀모도로 구간 타입", example = "FOCUS | BREAK | NORMAL")
        PomodoroPhaseType phaseType,

        @Schema(description = "뽀모도로 라운드", example = "2")
        Integer round,

        @Schema(description = "이번 phase 시작 시간", example = "")
        LocalDateTime phaseStartedAt
){
    public static PomodoroInfoResponse from(FocusSession focusSession, FocusInterval focusInterval) {
        return new PomodoroInfoResponse(
                focusSession.getFocusDuration(),
                focusSession.getBreakDuration(),
                focusSession.getRepeatCount(),
                focusInterval.getPhaseType(),
                focusInterval.getRound(),
                focusInterval.getStartedAt()
        );
    }
}