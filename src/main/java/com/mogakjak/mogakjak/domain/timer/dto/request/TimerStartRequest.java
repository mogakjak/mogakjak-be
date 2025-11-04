package com.mogakjak.mogakjak.domain.timer.dto.request;

import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record TimerStartRequest (
        @Schema(description = "타이머 모드", example = "TIMER | STOPWATCH | POMODORO")
        TimerMode timerMode,

        @Schema(description = "목표 시간(초 단위)", example = "1800")
        Long targetSeconds,

        @Schema(description = "뽀모도로 집중 시간(초 단위)", example = "1500")
        Long focusSeconds,

        @Schema(description = "뽀모도로 휴식 시간(초 단위)", example = "300")
        Long breakSeconds,

        @Schema(description = "뽀모도로 반복 횟수", example = "4")
        Integer repeatCount
) {}
