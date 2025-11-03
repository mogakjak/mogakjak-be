package com.mogakjak.mogakjak.domain.timer.controller;

import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerStopResponse;
import com.mogakjak.mogakjak.domain.timer.service.TimerService;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.resolver.CurrentUser;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/timers")
@RequiredArgsConstructor
public class TimerController {

    private final TimerService timerService;

    @PostMapping("/start")
    public ApiResponse<Void> startTimer(
            @CurrentUser User user,
            @RequestBody TimerStartRequest request
    ) {
        timerService.startTimer(user, request);
        return ApiResponse.success(SuccessCode.OK);
    }

    @PostMapping("/pause")
    public ApiResponse<Void> pauseTimer(@CurrentUser User user) {
        timerService.pauseTimer(user);
        return ApiResponse.success(SuccessCode.OK);
    }

    @PostMapping("/resume")
    public ApiResponse<Void> resumeTimer(@CurrentUser User user) {
        timerService.resumeTimer(user);
        return ApiResponse.success(SuccessCode.OK);
    }

    @PostMapping("/stop")
    public ApiResponse<TimerStopResponse> stopTimer(@CurrentUser User user) {
        TimerStopResponse response = timerService.stopTimer(user);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping("/{sessionId}/next-phase")
    public ApiResponse<Void> nextPomodoroPhase(
            @CurrentUser User user,
            @PathVariable UUID sessionId
    ) {
        timerService.nextPomodoroPhase(user, sessionId);
        return ApiResponse.success(SuccessCode.OK);
    }
}
