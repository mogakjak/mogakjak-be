package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;

import java.time.Duration;
import java.time.LocalDateTime;

public final class TodoAccumulatedTimeCalculator {

    private TodoAccumulatedTimeCalculator() {
    }

    /**
     * Todo에 저장된 누적 몰입 시간에 아직 저장되지 않은 현재 실행 구간을 더한다.
     * 일시정지/종료 구간은 이미 Todo에 반영되며, 뽀모도로 휴식 구간은 몰입 시간에서 제외한다.
     */
    public static long calculate(
            Todo todo,
            FocusSession focusSession,
            FocusInterval currentInterval,
            LocalDateTime now
    ) {
        long accumulatedSeconds = todo.getActualTimeInSeconds() != null
                ? todo.getActualTimeInSeconds().longValue()
                : 0L;

        if (focusSession.getStatus() != TimerStatus.RUNNING
                || currentInterval == null
                || currentInterval.getEndedAt() != null
                || currentInterval.getPhaseType() == PomodoroPhaseType.BREAK) {
            return accumulatedSeconds;
        }

        long currentIntervalSeconds = Duration.between(currentInterval.getStartedAt(), now).getSeconds();
        return accumulatedSeconds + Math.max(currentIntervalSeconds, 0L);
    }
}
