package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceService;
import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceSummary;
import com.mogakjak.mogakjak.domain.timer.service.FocusTimeAggregationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterGrowthServiceTest {

    @Mock private FocusAttendanceService focusAttendanceService;
    @Mock private FocusTimeAggregationService focusTimeAggregationService;

    @InjectMocks
    private CharacterGrowthService service;

    @Test
    void getStatus_combinesRetroactiveAttendanceAndCanonicalLifetimeFocusTime() {
        UUID userId = UUID.randomUUID();
        when(focusAttendanceService.getSummary(userId)).thenReturn(new FocusAttendanceSummary(30, Map.of()));
        when(focusTimeAggregationService.getLifetimeSeconds(userId)).thenReturn(50L * 3600);

        CharacterGrowthStatus status = service.getStatus(userId);

        assertEquals(30, status.attendanceDays());
        assertEquals(50L * 3600, status.totalFocusSeconds());
        assertEquals(status.totalFocusSeconds(), focusTimeAggregationService.getLifetimeSeconds(userId));
    }
}
