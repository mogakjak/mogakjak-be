package com.mogakjak.mogakjak.domain.feedback.dto.response;

import com.mogakjak.mogakjak.domain.feedback.entity.Feedback;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record FeedbackResponse(
        @Schema(description = "피드백 점수 (1-5 사이 정수만 가능)", example = "2")
        int score,

        @Schema(description = "작성한 피드백에 단 태그 코드 리스트", example = "[\"POS-001\", \"NEG-002\", \"NEU-003\"]")
        List<String> tagCodes,

        @Schema(description = "피드백 상세", example = "모각작 좋으네요.")
        String content
) {
    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getScore(),
                feedback.getTagRelations().stream()
                        .map(rel -> rel.getTag().getCode())
                        .toList(),
                feedback.getContent()
        );
    }
}
