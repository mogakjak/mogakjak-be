package com.mogakjak.mogakjak.domain.user.controller;

import com.mogakjak.mogakjak.domain.user.controller.dto.TimerTabOrderRequest;
import com.mogakjak.mogakjak.domain.user.controller.dto.TimerTabOrderResponse;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.service.UserTimerSettingService;
import com.mogakjak.mogakjak.global.auth.security.resolver.CurrentUser;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "UserTimerSetting", description = "타이머 설정 관련 API")
@RestController
@RequestMapping("/api/timer-settings")
@RequiredArgsConstructor
public class UserTimerSettingController {

    private final UserTimerSettingService userTimerSettingService;

    @Operation(summary = "타이머 탭 순서 조회")
    @GetMapping("/tab-order")
    public ApiResponse<TimerTabOrderResponse> getTimerTabOrder(
            @Parameter(hidden = true) @CurrentUser User user
    ) {
        return ApiResponse.success(SuccessCode.OK, userTimerSettingService.getTimerTabOrder(user));
    }

    @Operation(summary = "타이머 탭 순서 변경")
    @PatchMapping("/tab-order")
    public ApiResponse<Void> updateTimerTabOrder(
            @Parameter(hidden = true) @CurrentUser User user,
            @Valid @RequestBody TimerTabOrderRequest request
    ) {
        userTimerSettingService.updateTimerTabOrder(user, request);
        return ApiResponse.success(SuccessCode.OK);
    }
}