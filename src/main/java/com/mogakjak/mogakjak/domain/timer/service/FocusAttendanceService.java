package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusAttendanceService {

    public static final long DAILY_ATTENDANCE_SECONDS = 30 * 60;
    public static final ZoneId ATTENDANCE_ZONE = ZoneId.of("Asia/Seoul");

    private final FocusIntervalRepository focusIntervalRepository;

    public FocusAttendanceSummary getSummary(UUID userId) {
        Map<LocalDate, Long> dailyFocusSeconds = new HashMap<>();
        focusIntervalRepository.findCompletedFocusIntervalsByUser(
                        userId,
                        EnumSet.of(PomodoroPhaseType.FOCUS, PomodoroPhaseType.NORMAL)
                )
                .forEach(interval -> addBySeoulDate(interval, dailyFocusSeconds));

        long attendanceDays = dailyFocusSeconds.values().stream()
                .filter(seconds -> seconds >= DAILY_ATTENDANCE_SECONDS)
                .count();
        return new FocusAttendanceSummary(attendanceDays, Map.copyOf(dailyFocusSeconds));
    }

    private void addBySeoulDate(FocusInterval interval, Map<LocalDate, Long> dailyFocusSeconds) {
        if (interval.getStartedAt() == null || interval.getEndedAt() == null
                || !interval.getStartedAt().isBefore(interval.getEndedAt())) {
            return;
        }

        ZonedDateTime cursor = interval.getStartedAt().atZone(ATTENDANCE_ZONE);
        ZonedDateTime end = interval.getEndedAt().atZone(ATTENDANCE_ZONE);
        while (cursor.isBefore(end)) {
            ZonedDateTime nextDay = cursor.toLocalDate().plusDays(1).atStartOfDay(ATTENDANCE_ZONE);
            ZonedDateTime segmentEnd = nextDay.isBefore(end) ? nextDay : end;
            long seconds = Duration.between(cursor, segmentEnd).getSeconds();
            dailyFocusSeconds.merge(cursor.toLocalDate(), seconds, Long::sum);
            cursor = segmentEnd;
        }
    }
}
