package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class FocusSessionSnapshotCalculator {
    private FocusSessionSnapshotCalculator() {}

    public record Snapshot(FocusInterval currentInterval, long focusSeconds, long phaseSeconds, boolean isCounting) {}

    public static Snapshot calculate(FocusSession session, List<FocusInterval> intervals, LocalDateTime now) {
        FocusInterval current = intervals.stream().max(Comparator.comparing(FocusInterval::getStartedAt)).orElse(null);
        boolean isCounting = current != null && current.getEndedAt() == null
                && session.getStatus() == TimerStatus.RUNNING && !current.getStartedAt().isAfter(now);
        long focus = 0;
        long phase = 0;
        for (FocusInterval interval : intervals) {
            long seconds = 0;
            if (interval.getEndedAt() != null && !interval.getEndedAt().isAfter(now)) {
                seconds = Math.max(0, Duration.between(interval.getStartedAt(), interval.getEndedAt()).getSeconds());
            } else if (interval == current && isCounting) {
                seconds = Math.max(0, Duration.between(interval.getStartedAt(), now).getSeconds());
            }
            if (interval.getPhaseType() == PomodoroPhaseType.NORMAL || interval.getPhaseType() == PomodoroPhaseType.FOCUS) {
                focus += seconds;
            }
            if (current != null && interval.getPhaseType() == current.getPhaseType()
                    && Objects.equals(interval.getRound(), current.getRound())) {
                phase += seconds;
            }
        }
        return new Snapshot(current, focus, phase, isCounting);
    }
}
