package com.mogakjak.mogakjak.domain.feedback.controller;

import com.mogakjak.mogakjak.domain.feedback.dto.request.FeedbackCreateRequest;
import com.mogakjak.mogakjak.domain.feedback.dto.response.FeedbackResponse;
import com.mogakjak.mogakjak.domain.feedback.service.FeedbackService;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.resolver.CurrentUser;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feedback", description = "피드백 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "피드백 등록", description = "타이머 세션 종료 후 피드백을 등록합니다.")
    @PostMapping
    public ApiResponse<FeedbackResponse> createFeedback(
            @Parameter(hidden = true) @CurrentUser User user,
            @RequestBody FeedbackCreateRequest request
    ) {
        return ApiResponse.success(SuccessCode.OK, feedbackService.createFeedback(user, request));
    }
}
