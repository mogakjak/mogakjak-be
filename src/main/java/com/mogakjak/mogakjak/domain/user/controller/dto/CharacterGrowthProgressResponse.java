package com.mogakjak.mogakjak.domain.user.controller.dto;

import com.mogakjak.mogakjak.domain.user.service.CharacterGrowthPolicy;
import com.mogakjak.mogakjak.domain.user.service.CharacterGrowthStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "현재 보유 레벨에서 다음 레벨까지 두 조건 중 느린 쪽의 진행률(0~100)", example = "40.0")
    private double levelProgressRate;
    @Schema(description = "Lv.1~12의 11개 구간 전체 진행률(0~100). 표시용 정수는 클라이언트에서 버림", example = "30.90909090909091")
    private double overallProgressRate;
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
                        .levelProgressRate(levelProgressFraction(status, currentLevel, next) * 100)
                        .overallProgressRate(((currentLevel - 1) + levelProgressFraction(status, currentLevel, next))
                                / (CharacterGrowthPolicy.LEVEL_12.level() - 1) * 100)
                        .maxLevelReached(false)
                        .build())
                .orElseGet(() -> CharacterGrowthProgressResponse.builder()
                        .currentAttendanceDays(status.attendanceDays())
                        .currentFocusTimeInSeconds(status.totalFocusSeconds())
                        .currentLevel(currentLevel)
                        .attendanceProgressRate(100)
                        .focusTimeProgressRate(100)
                        .levelProgressRate(100)
                        .overallProgressRate(100)
                        .maxLevelReached(true)
                        .build());
    }

    private static double levelProgressFraction(
            CharacterGrowthStatus status, int currentLevel, CharacterGrowthPolicy next
    ) {
        CharacterGrowthPolicy current = CharacterGrowthPolicy.forLevel(currentLevel).orElseThrow();
        double attendance = (double) (status.attendanceDays() - current.requiredAttendanceDays())
                / (next.requiredAttendanceDays() - current.requiredAttendanceDays());
        double focusTime = (double) (status.totalFocusSeconds() - current.requiredFocusSeconds())
                / (next.requiredFocusSeconds() - current.requiredFocusSeconds());
        return Math.max(0, Math.min(1, Math.min(attendance, focusTime)));
    }
}
