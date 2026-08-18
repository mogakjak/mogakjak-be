package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TodoAccumulatedTimeCalculatorTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 8, 18, 12, 0);

    @Test
    void calculate_addsRunningIntervalToSavedTodoTime() {
        Todo todo = todoWithActualTime(3_600);
        FocusSession session = sessionWithStatus(TimerStatus.RUNNING);
        FocusInterval interval = interval(PomodoroPhaseType.NORMAL, now.minusSeconds(15), null);

        long result = TodoAccumulatedTimeCalculator.calculate(todo, session, interval, now);

        assertEquals(3_615L, result);
    }

    @Test
    void calculate_doesNotAddPausedIntervalAgain() {
        Todo todo = todoWithActualTime(3_600);
        FocusSession session = sessionWithStatus(TimerStatus.PAUSED);
        FocusInterval interval = interval(PomodoroPhaseType.FOCUS, now.minusSeconds(15), now);

        long result = TodoAccumulatedTimeCalculator.calculate(todo, session, interval, now);

        assertEquals(3_600L, result);
    }

    @Test
    void calculate_excludesPomodoroBreakFromFocusTime() {
        Todo todo = todoWithActualTime(3_600);
        FocusSession session = sessionWithStatus(TimerStatus.RUNNING);
        FocusInterval interval = interval(PomodoroPhaseType.BREAK, now.minusSeconds(15), null);

        long result = TodoAccumulatedTimeCalculator.calculate(todo, session, interval, now);

        assertEquals(3_600L, result);
    }

    @Test
    void calculate_clampsFutureIntervalToZero() {
        Todo todo = todoWithActualTime(3_600);
        FocusSession session = sessionWithStatus(TimerStatus.RUNNING);
        FocusInterval interval = interval(PomodoroPhaseType.NORMAL, now.plusSeconds(1), null);

        long result = TodoAccumulatedTimeCalculator.calculate(todo, session, interval, now);

        assertEquals(3_600L, result);
    }

    private Todo todoWithActualTime(int actualTimeInSeconds) {
        Todo todo = mock(Todo.class);
        when(todo.getActualTimeInSeconds()).thenReturn(actualTimeInSeconds);
        return todo;
    }

    private FocusSession sessionWithStatus(TimerStatus status) {
        FocusSession session = mock(FocusSession.class);
        when(session.getStatus()).thenReturn(status);
        return session;
    }

    private FocusInterval interval(
            PomodoroPhaseType phaseType,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
        FocusInterval interval = mock(FocusInterval.class);
        when(interval.getPhaseType()).thenReturn(phaseType);
        when(interval.getStartedAt()).thenReturn(startedAt);
        when(interval.getEndedAt()).thenReturn(endedAt);
        return interval;
    }
}
