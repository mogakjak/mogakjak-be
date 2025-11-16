package com.mogakjak.mogakjak.domain.timer.controller;

import com.mogakjak.mogakjak.domain.timer.dto.request.StopwatchStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerResponse;
import com.mogakjak.mogakjak.domain.timer.service.FocusSessionService;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.resolver.CurrentUser;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
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

    private final FocusSessionService focusSessionService;

    // 타이머
    @Operation(summary = "개인 타이머 시작", description = "개인 타이머를 시작합니다.")
    @PostMapping("/start/timer")
    public ApiResponse<TimerResponse> startTimer(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody TimerStartRequest request
    ) {
        TimerResponse response = focusSessionService.startTimer(user, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "개인 타이머 정지", description = "개인 타이머를 정지합니다.")
    @PostMapping("/pause/timer/{sessionId}")
    public ApiResponse<TimerResponse> pauseTimer(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID sessionId
    ) {
        TimerResponse response = focusSessionService.pauseTimer(user, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "개인 타이머 재개", description = "개인 타이머를 재개합니다.")
    @PostMapping("/resume/timer/{sessionId}")
    public ApiResponse<TimerResponse> resumeTimer(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID sessionId
    ) {
        TimerResponse response = focusSessionService.resumeTimer(user, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "개인 타이머 종료", description = "개인 타이머를 종료합니다.")
    @PostMapping("/finish/timer/{sessionId}")
    public ApiResponse<TimerResponse> finishTimer(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID sessionId
    ) {
        TimerResponse response = focusSessionService.finishTimer(user, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    // 스톱워치
    @Operation(summary = "개인 스톱워치 시작", description = "개인 스톱워치를 시작합니다.")
    @PostMapping("/start/stopwatch")
    public ApiResponse<TimerResponse> startStopwatch(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody StopwatchStartRequest request
    ) {
        TimerResponse response = focusSessionService.startStopWatch(user, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

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
