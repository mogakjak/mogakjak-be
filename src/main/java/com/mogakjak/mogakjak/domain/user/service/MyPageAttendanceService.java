package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceService;
import com.mogakjak.mogakjak.domain.timer.service.FocusAttendanceSummary;
import com.mogakjak.mogakjak.domain.user.controller.dto.MyPageAttendanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageAttendanceService {

    private final FocusAttendanceService focusAttendanceService;
    private final Clock clock;

    public MyPageAttendanceResponse getAttendance(UUID userId) {
        LocalDateTime asOf = LocalDateTime.now(clock.withZone(FocusAttendanceService.ATTENDANCE_ZONE));
        FocusAttendanceSummary summary = focusAttendanceService.getSummary(userId, asOf);
        return MyPageAttendanceResponse.from(summary, asOf.toLocalDate());
    }
}
