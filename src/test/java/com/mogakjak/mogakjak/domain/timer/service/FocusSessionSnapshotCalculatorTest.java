package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.entity.*;
import com.mogakjak.mogakjak.domain.timer.enumerate.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FocusSessionSnapshotCalculatorTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 5, 12, 0);
    private final UUID id = UUID.randomUUID();

    @Test
    void resumedSegmentsInSameRoundAreSummedWithoutPausedGap() {
        var session = mock(FocusSession.class);
        when(session.getStatus()).thenReturn(TimerStatus.RUNNING);
        var closed = interval(-100, -70, PomodoroPhaseType.FOCUS, 1);
        var open = interval(-40, null, PomodoroPhaseType.FOCUS, 1);
        var snapshot = FocusSessionSnapshotCalculator.calculate(session, List.of(open, closed), now);
        assertEquals(70, snapshot.focusSeconds());
        assertEquals(70, snapshot.phaseSeconds());
        assertTrue(snapshot.isCounting());
    }

    @Test
    void breakClockAdvancesButFocusCounterDoesNot() {
        var session = mock(FocusSession.class);
        when(session.getStatus()).thenReturn(TimerStatus.RUNNING);
        var snapshot = FocusSessionSnapshotCalculator.calculate(session, List.of(
                interval(-100, -70, PomodoroPhaseType.FOCUS, 1),
                interval(-60, -55, PomodoroPhaseType.BREAK, 1),
                interval(-20, null, PomodoroPhaseType.BREAK, 1)), now);
        assertEquals(30, snapshot.focusSeconds());
        assertEquals(25, snapshot.phaseSeconds());
        assertTrue(snapshot.isCounting());
    }

    @Test
    void pausedOpenIntervalFutureNegativeAndOlderOrphansAreIgnored() {
        var session = mock(FocusSession.class);
        when(session.getStatus()).thenReturn(TimerStatus.PAUSED);
        var snapshot = FocusSessionSnapshotCalculator.calculate(session, List.of(
                interval(-100, null, PomodoroPhaseType.FOCUS, 1),
                interval(-70, -80, PomodoroPhaseType.FOCUS, 1),
                interval(-50, 60, PomodoroPhaseType.FOCUS, 1),
                interval(10, null, PomodoroPhaseType.FOCUS, 2)), now);
        assertEquals(0, snapshot.focusSeconds());
        assertFalse(snapshot.isCounting());
    }

    @Test
    void currentRoundElapsedIsSeparateFromWholeSessionFocus() {
        var session = mock(FocusSession.class);
        when(session.getStatus()).thenReturn(TimerStatus.RUNNING);
        var snapshot = FocusSessionSnapshotCalculator.calculate(session, List.of(
                interval(-200, -140, PomodoroPhaseType.FOCUS, 1),
                interval(-120, -90, PomodoroPhaseType.BREAK, 1),
                interval(-30, null, PomodoroPhaseType.FOCUS, 2)), now);
        assertEquals(90, snapshot.focusSeconds());
        assertEquals(30, snapshot.phaseSeconds());
        assertFalse(FocusSessionSnapshotCalculator.calculate(session, List.of(), now).isCounting());
    }

    private FocusInterval interval(int start, Integer end, PomodoroPhaseType phase, int round) {
        var interval = FocusInterval.create(id, now.plusSeconds(start), phase, round);
        if (end != null) interval.end(now.plusSeconds(end));
        return interval;
    }
}
