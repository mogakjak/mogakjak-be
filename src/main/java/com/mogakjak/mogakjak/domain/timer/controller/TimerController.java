package com.mogakjak.mogakjak.domain.timer.controller;

import com.mogakjak.mogakjak.domain.timer.dto.request.PomodoroStartRequest;
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

    @Operation(summary = "개인 타이머 시작", description = "개인 타이머를 시작합니다.")
    @PostMapping("/start/timer")
    public ApiResponse<TimerResponse> startTimer(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody TimerStartRequest request
    ) {
        TimerResponse response = focusSessionService.startTimer(user, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "개인 스톱워치 시작", description = "개인 스톱워치를 시작합니다.")
    @PostMapping("/start/stopwatch")
    public ApiResponse<TimerResponse> startStopwatch(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody StopwatchStartRequest request
    ) {
        TimerResponse response = focusSessionService.startStopwatch(user, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "개인 뽀모도로 시작", description = "개인 뽀모도로를 시작합니다.")
    @PostMapping("/start/pomodoro")
    public ApiResponse<TimerResponse> startPomodoro(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody PomodoroStartRequest request
    ) {
        TimerResponse response = focusSessionService.startPomodoro(user, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "개인 타이머/스톱워치 정지", description = "개인 타이머/스톱워치를 정지합니다.")
    @PostMapping("/pause/{sessionId}")
    public ApiResponse<TimerResponse> pauseSession(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID sessionId
    ) {
        TimerResponse response = focusSessionService.pauseSession(user, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "개인 타이머/스톱워치 재개", description = "개인 타이머/스톱워치를 재개합니다.")
    @PostMapping("/resume/{sessionId}")
    public ApiResponse<TimerResponse> resumeSession(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID sessionId
    ) {
        TimerResponse response = focusSessionService.resumeSession(user, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "개인 타이머/스톱워치 종료", description = "개인 타이머/스톱워치를 종료합니다.")
    @PostMapping("/finish/{sessionId}")
    public ApiResponse<TimerResponse> finishSession(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID sessionId
    ) {
        TimerResponse response = focusSessionService.finishSession(user, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "개인 뽀모도로 다음 단계로 전환", description = "개인 뽀모도로를 다음 단계(FOCUS or BREAK)로 전환합니다. 마지막 라운드였던 경우, 뽀모도로 타이머가 종료됩니다.")
    @PostMapping("/next/pomodoro/{sessionId}")
    public ApiResponse<TimerResponse> nextPomodoroPhase(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID sessionId
    ) {
        TimerResponse response = focusSessionService.nextPomodoroPhase(user, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

//
//    @GetMapping("/statistics/daily")
//    public ApiResponse<List<DailyFocusStatsResponse>> getDailyStatistics(@Parameter(hidden = true) @CurrentUser User user) {
//        List<DailyFocusStatsResponse> stats = timerService.getDailyFocusDurations(user);
//        return ApiResponse.success(SuccessCode.OK, stats);
//    }
}
