package com.mogakjak.mogakjak.domain.timer.controller;

import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerPauseResponse;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerStartResponse;
import com.mogakjak.mogakjak.domain.timer.service.FocusSessionService;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.resolver.CurrentUser;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Timer", description = "타이머 관련 API")
@RestController
@RequestMapping("/api/timers")
@RequiredArgsConstructor
public class TimerController {

    private final FocusSessionService timerService;

    @PostMapping("/start/timer")
    public ApiResponse<TimerStartResponse> startTimer(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody TimerStartRequest request
    ) {
        TimerStartResponse response = timerService.startTimer(user, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping("/pause/timer/{sessionId}")
    public ApiResponse<TimerPauseResponse> pauseTimer(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID sessionId
    ) {
        TimerPauseResponse response = timerService.pauseTimer(user, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping("/resume/timer/{sessionId}")
    public ApiResponse<String> resumeTimer(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID sessionId
    ) {
        timerService.resumeTimer(user, sessionId);
        return ApiResponse.success(SuccessCode.OK, "타이머가 성공적으로 재실행 되었습니다.");
    }
//
//    @PostMapping("/stop/timer/{sessionId}")
//    public ApiResponse<String> stopTimer(
//            @Parameter(hidden = true) @CurrentUser User user,
//            @PathVariable UUID sessionId
//    ) {
//        TimerStopResponse response = timerService.stopTimer(user, sessionId);
//        return ApiResponse.success(SuccessCode.OK, "타이머가 성공적으로 중지 되었습니다.");
//    }

//    @PostMapping("/pause")
//    public ApiResponse<Void> pauseTimer(@Parameter(hidden = true) @CurrentUser User user) {
//        timerService.pauseTimer(user);
//        return ApiResponse.success(SuccessCode.OK);
//    }
//
//    @PostMapping("/resume")
//    public ApiResponse<Void> resumeTimer(@Parameter(hidden = true) @CurrentUser User user) {
//        timerService.resumeTimer(user);
//        return ApiResponse.success(SuccessCode.OK);
//    }
//
//    @PostMapping("/stop")
//    public ApiResponse<TimerStopResponse> stopTimer(@Parameter(hidden = true) @CurrentUser User user) {
//        TimerStopResponse response = timerService.stopTimer(user);
//        return ApiResponse.success(SuccessCode.OK, response);
//    }
//
//    @PostMapping("/{sessionId}/next-phase")
//    public ApiResponse<Void> nextPomodoroPhase(
//            @Parameter(hidden = true) @CurrentUser User user,
//            @PathVariable UUID sessionId
//    ) {
//        timerService.nextPomodoroPhase(user, sessionId);
//        return ApiResponse.success(SuccessCode.OK);
//    }
//
//    @GetMapping("/statistics/daily")
//    public ApiResponse<List<DailyFocusStatsResponse>> getDailyStatistics(@Parameter(hidden = true) @CurrentUser User user) {
//        List<DailyFocusStatsResponse> stats = timerService.getDailyFocusDurations(user);
//        return ApiResponse.success(SuccessCode.OK, stats);
//    }
}
