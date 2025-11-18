package com.mogakjak.mogakjak.domain.timer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record GroupTimerStartRequest(

        @NotNull
        @Schema(description = "목표 시간(초 단위)", example = "1800")
        Long targetSeconds

) {}
