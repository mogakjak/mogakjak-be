package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "그룹 타이머 공개/비공개 설정 요청")
public class GroupTimerVisibilityRequest {
    @NotNull(message = "공개 여부는 필수입니다.")
    @Schema(description = "그룹 타이머 공개 여부", example = "true")
    private Boolean isTimerPublic;
}

