package com.mogakjak.mogakjak.domain.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class CheckAwardRequest {
    @NotNull
    @Schema(description = "총 누적 학습 시간 (초 단위)", example = "36000")
    private Long totalStudyTimeInSeconds;
}
