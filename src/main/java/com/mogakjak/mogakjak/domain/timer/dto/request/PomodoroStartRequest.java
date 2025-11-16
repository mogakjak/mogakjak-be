package com.mogakjak.mogakjak.domain.timer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PomodoroStartRequest(

        @NotNull
        @Schema(description = "해당 타이머 세션의 todo Id", example = "7f000001-9a3d-1f34-819a-3d92e3800004")
        UUID todoId,

        @NotNull
        @Schema(description = "목표 시간(초 단위)", example = "1800")
        Long targetSeconds,

        @NotNull
        @Schema(description = "집중 시간(초 단위)", example = "1200")
        Long focusSeconds,

        @NotNull
        @Schema(description = "휴식 시간(초 단위)", example = "600")
        Long breakSeconds,

        @NotNull
        @Schema(description = "반복 횟수", example = "3")
        Integer repeatCount

) {}
