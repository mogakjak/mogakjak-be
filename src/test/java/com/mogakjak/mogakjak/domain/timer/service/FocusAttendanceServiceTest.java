package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FocusAttendanceServiceTest {

    @Mock
    private FocusIntervalRepository focusIntervalRepository;

    private FocusAttendanceService service;
    private static final LocalDateTime AS_OF = LocalDateTime.of(2026, 11, 5, 0, 0);

    @BeforeEach
    void setup() {
        service = new FocusAttendanceService(focusIntervalRepository,
                Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void getSummary_combinesPausedIntervalsAndCountsExactlyThirtyMinutes() {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 20);
        FocusInterval first = interval(date.atTime(9, 0), date.atTime(9, 20));
        FocusInterval second = interval(date.atTime(10, 0), date.atTime(10, 10));
        when(focusIntervalRepository.findCompletedFocusIntervalsByUser(
                userId,
                EnumSet.of(PomodoroPhaseType.FOCUS, PomodoroPhaseType.NORMAL), AS_OF
        )).thenReturn(List.of(first, second));

        FocusAttendanceSummary summary = service.getSummary(userId);

        assertEquals(1, summary.attendanceDays());
        assertEquals(1_800L, summary.dailyFocusSeconds().get(date));
    }

    @Test
    void getSummary_doesNotCountOneSecondBelowThreshold() {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 20);
        FocusInterval interval = interval(date.atStartOfDay(), date.atStartOfDay().plusSeconds(1_799));
        when(focusIntervalRepository.findCompletedFocusIntervalsByUser(
                userId,
                EnumSet.of(PomodoroPhaseType.FOCUS, PomodoroPhaseType.NORMAL), AS_OF
        )).thenReturn(List.of(interval));

        FocusAttendanceSummary summary = service.getSummary(userId);

        assertEquals(0, summary.attendanceDays());
        assertEquals(1_799L, summary.dailyFocusSeconds().get(date));
    }

    @Test
    void getSummary_splitsIntervalsAtAsiaSeoulMidnight() {
        UUID userId = UUID.randomUUID();
        LocalDate firstDay = LocalDate.of(2026, 8, 20);
        LocalDate secondDay = firstDay.plusDays(1);
        FocusInterval crossingMidnight = interval(firstDay.atTime(23, 50), secondDay.atTime(0, 20));
        FocusInterval secondDayInterval = interval(secondDay.atTime(1, 0), secondDay.atTime(1, 10));
        when(focusIntervalRepository.findCompletedFocusIntervalsByUser(
                userId,
                EnumSet.of(PomodoroPhaseType.FOCUS, PomodoroPhaseType.NORMAL), AS_OF
        )).thenReturn(List.of(crossingMidnight, secondDayInterval));

        FocusAttendanceSummary summary = service.getSummary(userId);

        assertEquals(1, summary.attendanceDays());
        assertEquals(600L, summary.dailyFocusSeconds().get(firstDay));
        assertEquals(1_800L, summary.dailyFocusSeconds().get(secondDay));
    }

    private FocusInterval interval(LocalDateTime startedAt, LocalDateTime endedAt) {
        FocusInterval interval = FocusInterval.create(UUID.randomUUID(), startedAt, PomodoroPhaseType.NORMAL, 1);
        interval.end(endedAt);
        return interval;
    }

    @Test
    void getSummary_usesSeoulCutoffEvenWhenClockIsUtc() {
        UUID userId = UUID.randomUUID();

        FocusAttendanceSummary summary = service.getSummary(userId);

        assertEquals(0, summary.attendanceDays());
        verify(focusIntervalRepository).findCompletedFocusIntervalsByUser(userId,
                EnumSet.of(PomodoroPhaseType.FOCUS, PomodoroPhaseType.NORMAL), AS_OF);
    }

    @Test
    void getSummary_countsNonConsecutiveDatesOnceEach() {
        UUID userId = UUID.randomUUID();
        LocalDate firstDay = LocalDate.of(2026, 8, 20);
        LocalDate laterDay = firstDay.plusDays(3);
        when(focusIntervalRepository.findCompletedFocusIntervalsByUser(userId,
                EnumSet.of(PomodoroPhaseType.FOCUS, PomodoroPhaseType.NORMAL), AS_OF))
                .thenReturn(List.of(interval(firstDay.atTime(9, 0), firstDay.atTime(10, 0)),
                        interval(firstDay.atTime(11, 0), firstDay.atTime(12, 0)),
                        interval(laterDay.atTime(9, 0), laterDay.atTime(9, 30))));

        FocusAttendanceSummary summary = service.getSummary(userId);

        assertEquals(2, summary.attendanceDays());
        assertEquals(7_200L, summary.dailyFocusSeconds().get(firstDay));
    }

    @Test
    void getSummary_splitsLeapDayAndDoesNotCreateAnExtraDayAtMidnight() {
        UUID userId = UUID.randomUUID();
        LocalDate leapDay = LocalDate.of(2024, 2, 29);
        when(focusIntervalRepository.findCompletedFocusIntervalsByUser(userId,
                EnumSet.of(PomodoroPhaseType.FOCUS, PomodoroPhaseType.NORMAL), AS_OF))
                .thenReturn(List.of(interval(leapDay.minusDays(1).atTime(23, 30), leapDay.atTime(0, 30)),
                        interval(leapDay.atTime(23, 30), leapDay.plusDays(1).atStartOfDay())));

        FocusAttendanceSummary summary = service.getSummary(userId);

        assertEquals(2, summary.attendanceDays());
        assertEquals(2, summary.dailyFocusSeconds().size());
        assertEquals(3_600L, summary.dailyFocusSeconds().get(leapDay));
    }
}
