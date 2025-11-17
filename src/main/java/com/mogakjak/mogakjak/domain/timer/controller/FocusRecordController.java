package com.mogakjak.mogakjak.domain.timer.controller;

import com.mogakjak.mogakjak.domain.timer.dto.response.DailyFocusStatsResponse;
import com.mogakjak.mogakjak.domain.timer.service.FocusRecordService;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.resolver.CurrentUser;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Record", description = "기록 관련 API")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class FocusRecordController {

    private final FocusRecordService focusRecordService;

    @Operation(summary = "해당 연도의 일일 기록 조회", description = "해당 연도의 1월 1일 - 12월 31일까지의 일일 기록을 일자 별로 초 단위로 계산하여 반환합니다. dayOfWeek은 요일로, 1(월)-7(일)을 의미합니다.")
    @GetMapping("/statistics/daily")
    public ApiResponse<List<DailyFocusStatsResponse>> getDailyStatistics(
            @Parameter(hidden = true) @CurrentUser User user
    ) {
        List<DailyFocusStatsResponse> stats = focusRecordService.getDailyFocusDurations(user);
        return ApiResponse.success(SuccessCode.OK, stats);
    }
}
