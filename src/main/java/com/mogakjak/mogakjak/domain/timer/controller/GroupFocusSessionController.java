package com.mogakjak.mogakjak.domain.timer.controller;

import com.mogakjak.mogakjak.domain.timer.dto.request.GroupTimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.service.GroupFocusSessionService;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerResponse;
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

@Tag(name = "GroupTimer", description = "그룹 타이머 관련 API")
@RestController
@RequestMapping("/api/timers")
@RequiredArgsConstructor
public class GroupFocusSessionController {

    private final GroupFocusSessionService groupFocusSessionService;

    @Operation(summary = "그룹 타이머 시작", description = "그룹 타이머를 시작합니다.")
    @PostMapping("/start/timer/{groupId}")
    public ApiResponse<TimerResponse> startGroupTimer(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID groupId,
            @RequestBody GroupTimerStartRequest request
    ) {
        TimerResponse response = groupFocusSessionService.startGroupTimer(user, groupId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "그룹 타이머 정지", description = "그룹 타이머를 정지합니다.")
    @PostMapping("/pause/{sessionId}/{groupId}")
    public ApiResponse<TimerResponse> pauseSession(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID groupId,
            @PathVariable UUID sessionId
    ) {
        TimerResponse response = groupFocusSessionService.pauseSession(user, groupId, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "그룹 타이머 재개", description = "그룹 타이머를 재개합니다.")
    @PostMapping("/resume/{sessionId}/{groupId}")
    public ApiResponse<TimerResponse> resumeSession(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID groupId,
            @PathVariable UUID sessionId
    ) {
        TimerResponse response = groupFocusSessionService.resumeSession(user, groupId, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "그룹 타이머 종료", description = "그룹 타이머를 종료합니다.")
    @PostMapping("/finish/{sessionId}/{groupId}")
    public ApiResponse<TimerResponse> finishSession(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID groupId,
            @PathVariable UUID sessionId
    ) {
        TimerResponse response = groupFocusSessionService.finishSession(user, groupId, sessionId);
        return ApiResponse.success(SuccessCode.OK, response);
    }
}
