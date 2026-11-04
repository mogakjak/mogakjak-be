package com.mogakjak.mogakjak.domain.user.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceService;
import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "KST 기준 과거 5일·오늘·미래 4일의 출석 현황")
public record MyPageAttendanceResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate referenceDate,
        String timeZone,
        long attendanceThresholdInSeconds,
        long totalAttendanceDays,
        List<Day> days
) {
    public static MyPageAttendanceResponse from(FocusAttendanceSummary summary, LocalDate referenceDate) {
        List<Day> days = referenceDate.minusDays(5).datesUntil(referenceDate.plusDays(5))
                .map(date -> {
                    boolean future = date.isAfter(referenceDate);
                    long seconds = future ? 0 : summary.dailyFocusSeconds().getOrDefault(date, 0L);
                    return new Day(date, seconds,
                            !future && seconds >= FocusAttendanceService.DAILY_ATTENDANCE_SECONDS,
                            date.equals(referenceDate), future);
                })
                .toList();
        return new MyPageAttendanceResponse(referenceDate, FocusAttendanceService.ATTENDANCE_ZONE.getId(),
                FocusAttendanceService.DAILY_ATTENDANCE_SECONDS, summary.attendanceDays(), days);
    }

    public record Day(
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate date,
            long focusTimeInSeconds, boolean attended, boolean today, boolean future
    ) {
    }
}
