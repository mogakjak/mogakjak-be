package com.mogakjak.mogakjak.domain.timer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TimerStartRequest (

        @NotNull
        @Schema(description = "해당 타이머 세션의 todo Id", example = "7f000001-9a3d-1f34-819a-3d92e3800004")
        UUID todoId,

        @NotNull
        @Schema(description = "목표 시간(초 단위)", example = "1800")
        Long targetSeconds

) {}
