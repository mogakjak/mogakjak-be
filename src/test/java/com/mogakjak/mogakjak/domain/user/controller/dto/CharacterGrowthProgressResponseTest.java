package com.mogakjak.mogakjak.domain.user.controller.dto;

import com.mogakjak.mogakjak.domain.user.service.CharacterGrowthPolicy;
import com.mogakjak.mogakjak.domain.user.service.CharacterGrowthStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CharacterGrowthProgressResponseTest {
    @Test
    void levelFourExampleUsesAttendanceAsSlowerConditionAndKeepsFractionalPrecision() {
        var response = CharacterGrowthProgressResponse.from(new CharacterGrowthStatus(60, 120 * 3_600L), 4);

        assertEquals(4, response.getCurrentLevel());
        assertEquals(5, response.getNextLevel());
        assertEquals(40, response.getLevelProgressRate(), 1e-9);
        assertEquals(30.90909090909091, response.getOverallProgressRate(), 1e-9);
        assertEquals(15, response.getRemainingAttendanceDays());
        assertEquals(20 * 3_600L, response.getRemainingFocusTimeInSeconds());
        // 기존 누적 기준 필드는 하위 호환을 위해 유지한다.
        assertEquals(80, response.getAttendanceProgressRate());
        assertEquals(85, response.getFocusTimeProgressRate());
    }

    @Test
    void usesFocusTimeWhenAttendanceIsAlreadySatisfied() {
        var response = CharacterGrowthProgressResponse.from(new CharacterGrowthStatus(100, 100 * 3_600L), 4);

        assertEquals(20, response.getLevelProgressRate(), 1e-9);
        assertEquals(3.2 / 11 * 100, response.getOverallProgressRate(), 1e-9);
        assertEquals(0, response.getRemainingAttendanceDays());
        assertEquals(40 * 3_600L, response.getRemainingFocusTimeInSeconds());
    }

    @Test
    void oneSatisfiedConditionDoesNotAdvancePastUnmetCondition() {
        var response = CharacterGrowthProgressResponse.from(new CharacterGrowthStatus(50, 1_000 * 3_600L), 4);

        assertEquals(0, response.getLevelProgressRate());
        assertEquals(3.0 / 11 * 100, response.getOverallProgressRate(), 1e-9);
    }

    @Test
    void legacyOwnershipIsPreservedAndSegmentCannotBecomeNegative() {
        var response = CharacterGrowthProgressResponse.from(new CharacterGrowthStatus(0, 0), 5);

        assertEquals(5, response.getCurrentLevel());
        assertEquals(6, response.getNextLevel());
        assertEquals(0, response.getLevelProgressRate());
        assertEquals(4.0 / 11 * 100, response.getOverallProgressRate(), 1e-9);
        assertEquals(105, response.getRemainingAttendanceDays());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11})
    void clampsCompletedSegmentUntilNextCharacterIsActuallyAwarded(int currentLevel) {
        var next = CharacterGrowthPolicy.forLevel(currentLevel + 1).orElseThrow();
        var response = CharacterGrowthProgressResponse.from(new CharacterGrowthStatus(
                next.requiredAttendanceDays() + 5, next.requiredFocusSeconds() + 3_600), currentLevel);

        assertEquals(currentLevel, response.getCurrentLevel());
        assertEquals(currentLevel + 1, response.getNextLevel());
        assertEquals(100, response.getLevelProgressRate());
        assertEquals(currentLevel / 11.0 * 100, response.getOverallProgressRate(), 1e-9);
        assertEquals(0, response.getRemainingAttendanceDays());
        assertEquals(0, response.getRemainingFocusTimeInSeconds());
        assertFalse(response.isMaxLevelReached());
    }

    @Test
    void newUserStartsAtZeroAndDoesNotLosePrecisionBeforeOverallCalculation() {
        var empty = CharacterGrowthProgressResponse.from(new CharacterGrowthStatus(0, 0), 1);
        assertEquals(0, empty.getOverallProgressRate());
        var progressing = CharacterGrowthProgressResponse.from(new CharacterGrowthStatus(7, 10 * 3_600L), 1);
        assertEquals((7.0 / 15) / 11 * 100, progressing.getOverallProgressRate(), 1e-9);
    }

    @Test
    void maxLevelHasNoNextTargetButLifetimeCountersContinueGrowing() {
        var response = CharacterGrowthProgressResponse.from(new CharacterGrowthStatus(500, 1_500 * 3_600L), 12);

        assertTrue(response.isMaxLevelReached());
        assertEquals(100, response.getLevelProgressRate());
        assertEquals(100, response.getOverallProgressRate());
        assertEquals(500, response.getCurrentAttendanceDays());
        assertEquals(1_500 * 3_600L, response.getCurrentFocusTimeInSeconds());
        assertNull(response.getNextLevel());
        assertNull(response.getRequiredAttendanceDays());
        assertNull(response.getRequiredFocusTimeInSeconds());
        assertNull(response.getRemainingAttendanceDays());
        assertNull(response.getRemainingFocusTimeInSeconds());
    }
}
