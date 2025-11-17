package com.mogakjak.mogakjak.domain.feedback.dto.response;

import com.mogakjak.mogakjak.domain.feedback.entity.FeedbackTag;
import com.mogakjak.mogakjak.domain.feedback.enumerate.FeedbackTagType;
import io.swagger.v3.oas.annotations.media.Schema;

public record FeedbackTagResponse(

        @Schema(description = "피드백 태그 코드", example = "POS-001")
        String code,

        @Schema(description = "피드백 태그 내용", example = "스스로 시간을 관리하는 데 도움이 됐어요")
        String displayName,

        @Schema(description = "피드백 태그 종류", example = "POSITIVE | NEGATIVE | NEUTRAL")
        FeedbackTagType type

) {
    public static FeedbackTagResponse from(FeedbackTag tag) {
        return new FeedbackTagResponse(
                tag.getCode(),
                tag.getDisplayName(),
                tag.getType()
        );
    }
}
