package com.mogakjak.mogakjak.domain.timer.service;

public record FocusTimeMetrics(
        long totalSeconds,
        long groupSeconds,
        long personalSeconds
) {
}
