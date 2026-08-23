package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FocusAttendanceServiceTest {

    @Mock
    private FocusIntervalRepository focusIntervalRepository;

    @InjectMocks
    private FocusAttendanceService service;

    @Test
    void getSummary_combinesPausedIntervalsAndCountsExactlyThirtyMinutes() {
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 20);
        FocusInterval first = interval(date.atTime(9, 0), date.atTime(9, 20));
        FocusInterval second = interval(date.atTime(10, 0), date.atTime(10, 10));
        when(focusIntervalRepository.findCompletedFocusIntervalsByUser(
                userId,
                EnumSet.of(PomodoroPhaseType.FOCUS, PomodoroPhaseType.NORMAL)
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
                EnumSet.of(PomodoroPhaseType.FOCUS, PomodoroPhaseType.NORMAL)
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
                EnumSet.of(PomodoroPhaseType.FOCUS, PomodoroPhaseType.NORMAL)
        )).thenReturn(List.of(crossingMidnight, secondDayInterval));

        FocusAttendanceSummary summary = service.getSummary(userId);

        assertEquals(1, summary.attendanceDays());
        assertEquals(600L, summary.dailyFocusSeconds().get(firstDay));
        assertEquals(1_800L, summary.dailyFocusSeconds().get(secondDay));
    }

    private FocusInterval interval(LocalDateTime startedAt, LocalDateTime endedAt) {
        FocusInterval interval = mock(FocusInterval.class);
        when(interval.getStartedAt()).thenReturn(startedAt);
        when(interval.getEndedAt()).thenReturn(endedAt);
        return interval;
    }
}
