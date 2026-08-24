package com.mogakjak.mogakjak.domain.user.controller.dto;

import com.mogakjak.mogakjak.domain.user.service.CharacterGrowthPolicy;
import com.mogakjak.mogakjak.domain.user.service.CharacterGrowthStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CharacterGrowthProgressResponse {
    private long currentAttendanceDays;
    private long currentFocusTimeInSeconds;
    private int currentLevel;
    private Integer nextLevel;
    private Long requiredAttendanceDays;
    private Long requiredFocusTimeInSeconds;
    private Long remainingAttendanceDays;
    private Long remainingFocusTimeInSeconds;
    private int attendanceProgressRate;
    private int focusTimeProgressRate;
    private boolean maxLevelReached;

    public static CharacterGrowthProgressResponse from(CharacterGrowthStatus status, int currentLevel) {
        return CharacterGrowthPolicy.nextAfter(currentLevel)
                .map(next -> CharacterGrowthProgressResponse.builder()
                        .currentAttendanceDays(status.attendanceDays())
                        .currentFocusTimeInSeconds(status.totalFocusSeconds())
                        .currentLevel(currentLevel)
                        .nextLevel(next.level())
                        .requiredAttendanceDays(next.requiredAttendanceDays())
                        .requiredFocusTimeInSeconds(next.requiredFocusSeconds())
                        .remainingAttendanceDays(Math.max(0, next.requiredAttendanceDays() - status.attendanceDays()))
                        .remainingFocusTimeInSeconds(Math.max(0, next.requiredFocusSeconds() - status.totalFocusSeconds()))
                        .attendanceProgressRate(next.attendanceProgressRate(status))
                        .focusTimeProgressRate(next.focusTimeProgressRate(status))
                        .maxLevelReached(false)
                        .build())
                .orElseGet(() -> CharacterGrowthProgressResponse.builder()
                        .currentAttendanceDays(status.attendanceDays())
                        .currentFocusTimeInSeconds(status.totalFocusSeconds())
                        .currentLevel(currentLevel)
                        .attendanceProgressRate(100)
                        .focusTimeProgressRate(100)
                        .maxLevelReached(true)
                        .build());
    }
}
