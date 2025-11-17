package com.mogakjak.mogakjak.domain.feedback.dto.request;

import com.mogakjak.mogakjak.domain.feedback.enumerate.FeedbackTagType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record FeedbackTagCreateRequest(

        @NotNull
        @Schema(description = "피드백 태그 코드", example = "POS-001")
        String code,

        @NotNull
        @Schema(description = "피드백 태그 내용", example = "스스로 시간을 관리하는 데 도움이 됐어요")
        String displayName,

        @NotNull
        @Schema(description = "피드백 태그 종류", example = "POSITIVE | NEGATIVE | NEUTRAL")
        FeedbackTagType type

) {}
