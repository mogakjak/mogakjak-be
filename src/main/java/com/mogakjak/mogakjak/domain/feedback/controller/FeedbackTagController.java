package com.mogakjak.mogakjak.domain.feedback.controller;

import com.mogakjak.mogakjak.domain.feedback.dto.request.FeedbackTagCreateRequest;
import com.mogakjak.mogakjak.domain.feedback.dto.response.FeedbackTagResponse;
import com.mogakjak.mogakjak.domain.feedback.enumerate.FeedbackTagType;
import com.mogakjak.mogakjak.domain.feedback.service.FeedbackTagService;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Feedback", description = "피드백 관련 API")
@RestController
@RequestMapping("/api/feedback/tags")
@RequiredArgsConstructor
public class FeedbackTagController {

    private final FeedbackTagService feedbackTagService;

    @Operation(summary = "(운영용) 피드백 태그 일괄 등록", description = "피드백 태그를 일괄 등록합니다.")
    @PostMapping("/batch")
    public ApiResponse<Void> registerTags(
            @RequestBody List<FeedbackTagCreateRequest> requests
    ) {
        feedbackTagService.registerTags(requests);
        return ApiResponse.success(SuccessCode.CREATED);
    }

    @Operation(summary = "피드백 태그 전체 조회 및 필터링", description = "서버에 등록된 전체 피드백 태그를 리스트로 조회합니다. type(NEGATIVE | POSITIVE | NEUTRAL)을 쿼리파라미터로 입력 시 해당하는 태그만 출력됩니다.")
    @GetMapping
    public ApiResponse<List<FeedbackTagResponse>> getAllTags(
            @RequestParam(required = false) FeedbackTagType type
    ) {
        return ApiResponse.success(SuccessCode.OK, feedbackTagService.getAllTags(type));
    }
}
