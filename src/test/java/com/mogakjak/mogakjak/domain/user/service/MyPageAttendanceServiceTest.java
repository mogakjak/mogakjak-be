package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceService;
import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceSummary;
import com.mogakjak.mogakjak.domain.user.controller.dto.MyPageAttendanceResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyPageAttendanceServiceTest {
    private final FocusAttendanceService attendance = mock(FocusAttendanceService.class);
    private final UUID userId = UUID.randomUUID();
    private final MyPageAttendanceService service = new MyPageAttendanceService(attendance,
            Clock.fixed(Instant.parse("2026-10-18T15:05:00Z"), ZoneOffset.UTC));
    private final LocalDate today = LocalDate.of(2026, 10, 19);

    @Test
    void calendarAndLifetimeCountUseOneSeoulSnapshot() {
        when(attendance.getSummary(userId, LocalDateTime.of(2026, 10, 19, 0, 5)))
                .thenReturn(new FocusAttendanceSummary(25, Map.of(
                        today.minusDays(5), 1_800L, today.minusDays(1), 1_799L, today, 300L)));

        MyPageAttendanceResponse response = service.getAttendance(userId);

        assertEquals(today, response.referenceDate());
        assertEquals("Asia/Seoul", response.timeZone());
        assertEquals(1_800, response.attendanceThresholdInSeconds());
        assertEquals(25, response.totalAttendanceDays());
        assertEquals(10, response.days().size());
        assertEquals(today.minusDays(5), response.days().getFirst().date());
        assertEquals(today.plusDays(4), response.days().getLast().date());
        assertTrue(response.days().getFirst().attended());
        assertFalse(response.days().get(4).attended());
        assertEquals(1_799, response.days().get(4).focusTimeInSeconds());
        assertTrue(response.days().get(5).today());
        assertFalse(response.days().get(5).attended());
        assertEquals(1, response.days().stream().filter(MyPageAttendanceResponse.Day::today).count());
        assertTrue(response.days().stream().skip(6).allMatch(day -> day.future()
                && !day.attended() && day.focusTimeInSeconds() == 0));
        verify(attendance).getSummary(userId, LocalDateTime.of(2026, 10, 19, 0, 5));
    }

    @Test
    void todayHighlightIsIndependentOfAttendanceAndFutureDaysNeverCount() {
        MyPageAttendanceResponse response = MyPageAttendanceResponse.from(new FocusAttendanceSummary(1,
                Map.of(today, 1_800L, today.plusDays(1), 3_600L)), today);

        assertTrue(response.days().get(5).today());
        assertTrue(response.days().get(5).attended());
        assertFalse(response.days().get(5).future());
        assertEquals(0, response.days().get(6).focusTimeInSeconds());
        assertFalse(response.days().get(6).attended());
    }

    @Test
    void newUserGetsTenEmptyDaysAcrossYearBoundary() {
        LocalDate newYear = LocalDate.of(2027, 1, 1);
        MyPageAttendanceResponse response = MyPageAttendanceResponse.from(
                new FocusAttendanceSummary(0, Map.of()), newYear);

        assertEquals(LocalDate.of(2026, 12, 27), response.days().getFirst().date());
        assertEquals(LocalDate.of(2027, 1, 5), response.days().getLast().date());
        assertEquals(0, response.totalAttendanceDays());
        assertTrue(response.days().stream().noneMatch(MyPageAttendanceResponse.Day::attended));
    }
}
