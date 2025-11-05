package com.mogakjak.mogakjak.domain.timer.dto.response;

import java.time.LocalDate;

public record DailyFocusStatsResponse(
        LocalDate date,
        Long totalSeconds
) {}
