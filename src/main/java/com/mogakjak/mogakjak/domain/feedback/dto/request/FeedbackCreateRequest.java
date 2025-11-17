package com.mogakjak.mogakjak.domain.feedback.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record FeedbackCreateRequest(

        @NotNull
        @Schema(description = "타이머 세션 ID")
        UUID sessionId,

        @Min(1) @Max(5)
        @Schema(description = "점수 (1~5)")
        int score,

        @NotNull
        @Schema(description = "태그 코드 리스트(긍정:POS, 부정:NEG, 중립:NEU)", example = "[\"POS-001\", \"NEG-002\", \"NEU-003\"]")
        List<String> tagCodes,

        @Schema(description = "상세 자유 입력")
        String content

) {}
