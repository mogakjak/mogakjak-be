package com.mogakjak.mogakjak.domain.timer.controller;

import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.DailyFocusStatsResponse;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerStopResponse;
import com.mogakjak.mogakjak.domain.timer.service.TimerService;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.resolver.CurrentUser;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/timers")
@RequiredArgsConstructor
public class TimerController {

    private final TimerService timerService;

    @PostMapping("/start")
    public ApiResponse<Void> startTimer(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody TimerStartRequest request
    ) {
        timerService.startTimer(user, request);
        return ApiResponse.success(SuccessCode.OK);
    }

    @PostMapping("/pause")
    public ApiResponse<Void> pauseTimer(@Parameter(hidden = true) @CurrentUser User user) {
        timerService.pauseTimer(user);
        return ApiResponse.success(SuccessCode.OK);
    }

    @PostMapping("/resume")
    public ApiResponse<Void> resumeTimer(@Parameter(hidden = true) @CurrentUser User user) {
        timerService.resumeTimer(user);
        return ApiResponse.success(SuccessCode.OK);
    }

    @PostMapping("/stop")
    public ApiResponse<TimerStopResponse> stopTimer(@Parameter(hidden = true) @CurrentUser User user) {
        TimerStopResponse response = timerService.stopTimer(user);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping("/{sessionId}/next-phase")
    public ApiResponse<Void> nextPomodoroPhase(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID sessionId
    ) {
        timerService.nextPomodoroPhase(user, sessionId);
        return ApiResponse.success(SuccessCode.OK);
    }

    @GetMapping("/statistics/daily")
    public ApiResponse<List<DailyFocusStatsResponse>> getDailyStatistics(@Parameter(hidden = true) @CurrentUser User user) {
        List<DailyFocusStatsResponse> stats = timerService.getDailyFocusDurations(user);
        return ApiResponse.success(SuccessCode.OK, stats);
    }
}
