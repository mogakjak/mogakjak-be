package com.mogakjak.mogakjak.domain.timer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record DailyFocusStatsResponse(

        @Schema(description = "날짜", example = "2025-11-17")
        LocalDate date,

        @Schema(description = "몰입 시간(초 단위)", example = "197")
        Long totalSeconds

) {}
