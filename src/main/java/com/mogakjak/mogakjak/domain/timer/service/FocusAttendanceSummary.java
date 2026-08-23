package com.mogakjak.mogakjak.domain.timer.service;

import java.time.LocalDate;
import java.util.Map;

public record FocusAttendanceSummary(
        long attendanceDays,
        Map<LocalDate, Long> dailyFocusSeconds
) {
}
