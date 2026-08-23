package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceService;
import com.mogakjak.mogakjak.domain.timer.service.FocusTimeAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterGrowthService {

    private final FocusAttendanceService focusAttendanceService;
    private final FocusTimeAggregationService focusTimeAggregationService;

    public CharacterGrowthStatus getStatus(UUID userId) {
        long attendanceDays = focusAttendanceService.getSummary(userId).attendanceDays();
        long totalFocusSeconds = focusTimeAggregationService.getLifetimeSeconds(userId);
        return new CharacterGrowthStatus(attendanceDays, totalFocusSeconds);
    }
}
