package com.mogakjak.mogakjak.domain.timer.dto.response;

import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TimerStopResponse(

        @Schema(description = "타이머 세션 ID")
        UUID sessionId,

        @Schema(description = "타이머 모드", example = "TIMER | STOPWATCH | POMODORO")
        TimerMode mode,

        @Schema(description = "세션 상태", example = "FINISHED")
        TimerStatus status,

        @Schema(description = "시작 시각")
        LocalDateTime startedAt,

        @Schema(description = "종료 시각")
        LocalDateTime endedAt,

        @Schema(description = "목표 시간(초 단위)")
        Long targetDuration,

        @Schema(description = "총 집중 시간(초 단위)")
        Long totalDuration
) {}

