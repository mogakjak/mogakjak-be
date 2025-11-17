package com.mogakjak.mogakjak.domain.timer.util;

import com.mogakjak.mogakjak.domain.timer.enumerate.DashboardRangeType;

import java.time.*;

public class DashboardRange {

    private final LocalDateTime start;
    private final LocalDateTime end;

    private DashboardRange(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }

    public static DashboardRange of(DashboardRangeType type) {
        LocalDate today = LocalDate.now();

        return switch (type) {
            case TODAY -> {
                LocalDateTime s = today.atStartOfDay();
                LocalDateTime e = today.plusDays(1).atStartOfDay().minusNanos(1);
                yield new DashboardRange(s, e);
            }
            case WEEK -> {
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                LocalDateTime s = monday.atStartOfDay();
                LocalDateTime e = monday.plusDays(7).atStartOfDay().minusNanos(1);
                yield new DashboardRange(s, e);
            }
            case MONTH -> {
                LocalDate firstDay = today.withDayOfMonth(1);
                LocalDateTime s = firstDay.atStartOfDay();
                LocalDateTime e = firstDay.plusMonths(1).atStartOfDay().minusNanos(1);
                yield new DashboardRange(s, e);
            }
            case ALL -> {
                // 사실상 전체 기간 → 유저 가입일 사용해도 되고 그냥 최소값 사용
                LocalDateTime s = LocalDate.of(1970, 1, 1).atStartOfDay();
                LocalDateTime e = LocalDateTime.now();
                yield new DashboardRange(s, e);
            }
        };
    }

    public LocalDateTime start() { return start; }
    public LocalDateTime end() { return end; }
}
