package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GroupGoalRequest(

        @NotNull
        @Min(0) @Max(23)
        @Schema(description = "목표 시간 - 시간 (최소: 0, 최대:23)", example = "3")
        Integer hour,

        @NotNull
        @Min(0) @Max(59)
        @Schema(description = "목표 시간 - 분 (최소: 0, 최대:59)", example = "30")
        Integer minute

) {}
