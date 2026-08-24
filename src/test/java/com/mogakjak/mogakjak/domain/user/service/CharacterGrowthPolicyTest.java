package com.mogakjak.mogakjak.domain.user.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterGrowthPolicyTest {

    @Test
    void allTwelveLevelsMatchPolicy20Table() {
        long[] attendanceDays = {0, 15, 30, 50, 75, 105, 140, 180, 225, 275, 330, 400};
        long[] focusHours = {0, 20, 50, 90, 140, 200, 280, 380, 500, 650, 850, 1100};

        assertEquals(12, CharacterGrowthPolicy.all().size());
        for (int index = 0; index < 12; index++) {
            CharacterGrowthPolicy policy = CharacterGrowthPolicy.forLevel(index + 1).orElseThrow();
            assertEquals(attendanceDays[index], policy.requiredAttendanceDays());
            assertEquals(focusHours[index] * 3600, policy.requiredFocusSeconds());
        }
    }

    @Test
    void level12_matchesPolicy20Requirements() {
        CharacterGrowthPolicy policy = CharacterGrowthPolicy.LEVEL_12;

        assertEquals(400, policy.requiredAttendanceDays());
        assertEquals(1_100L * 3600, policy.requiredFocusSeconds());
        assertTrue(policy.isSatisfiedBy(new CharacterGrowthStatus(400, 1_100L * 3600)));
    }

    @Test
    void bothAttendanceAndFocusTimeAreRequired() {
        CharacterGrowthPolicy policy = CharacterGrowthPolicy.LEVEL_2;

        assertFalse(policy.isSatisfiedBy(new CharacterGrowthStatus(14, 100L * 3600)));
        assertFalse(policy.isSatisfiedBy(new CharacterGrowthStatus(100, 19L * 3600)));
        assertTrue(policy.isSatisfiedBy(new CharacterGrowthStatus(15, 20L * 3600)));
    }
}
