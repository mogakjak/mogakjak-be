package com.mogakjak.mogakjak.domain.timer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

public record DailyFocusStatsResponse(

        @Schema(description = "날짜", example = "2025-11-17")
        LocalDate date,

        @Schema(description = "몰입 시간(초 단위)", example = "197")
        Long totalSeconds,

        @Min(1) @Max(7)
        @Schema(description = "요일 (1(월) ~ 7(일))", example = "197")
        Integer dayOfWeek

) {
    public static DailyFocusStatsResponse from(LocalDate date, Long totalSeconds, Integer dayOfWeek) {
        return new DailyFocusStatsResponse(
                date,
                totalSeconds,
                dayOfWeek
        );
    }
}
