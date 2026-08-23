package com.mogakjak.mogakjak.domain.user.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public enum CharacterGrowthPolicy {
    LEVEL_1(1, 0, 0),
    LEVEL_2(2, 15, 20),
    LEVEL_3(3, 30, 50),
    LEVEL_4(4, 50, 90),
    LEVEL_5(5, 75, 140),
    LEVEL_6(6, 105, 200),
    LEVEL_7(7, 140, 280),
    LEVEL_8(8, 180, 380),
    LEVEL_9(9, 225, 500),
    LEVEL_10(10, 275, 650),
    LEVEL_11(11, 330, 850),
    LEVEL_12(12, 400, 1100);

    private final int level;
    private final long requiredAttendanceDays;
    private final long requiredFocusSeconds;

    CharacterGrowthPolicy(int level, long requiredAttendanceDays, long requiredFocusHours) {
        this.level = level;
        this.requiredAttendanceDays = requiredAttendanceDays;
        this.requiredFocusSeconds = requiredFocusHours * 3600;
    }

    public int level() {
        return level;
    }

    public long requiredAttendanceDays() {
        return requiredAttendanceDays;
    }

    public long requiredFocusSeconds() {
        return requiredFocusSeconds;
    }

    public boolean isSatisfiedBy(CharacterGrowthStatus status) {
        return status.attendanceDays() >= requiredAttendanceDays
                && status.totalFocusSeconds() >= requiredFocusSeconds;
    }

    public int attendanceProgressRate(CharacterGrowthStatus status) {
        return progressRate(status.attendanceDays(), requiredAttendanceDays);
    }

    public int focusTimeProgressRate(CharacterGrowthStatus status) {
        return progressRate(status.totalFocusSeconds(), requiredFocusSeconds);
    }

    public static Optional<CharacterGrowthPolicy> forLevel(int level) {
        return Arrays.stream(values()).filter(policy -> policy.level == level).findFirst();
    }

    public static Optional<CharacterGrowthPolicy> nextAfter(int level) {
        return all().stream().filter(policy -> policy.level > level).findFirst();
    }

    public static List<CharacterGrowthPolicy> all() {
        return Arrays.stream(values()).sorted(Comparator.comparingInt(CharacterGrowthPolicy::level)).toList();
    }

    private static int progressRate(long current, long required) {
        if (required == 0) {
            return 100;
        }
        return (int) Math.min(100, current * 100.0 / required);
    }
}
