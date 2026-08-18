package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record GroupFocusCheckRequest(
        @NotNull
        @Schema(description = "내 그룹 집중 체크 알림 수신 여부", example = "true")
        Boolean enabled
) {
}
