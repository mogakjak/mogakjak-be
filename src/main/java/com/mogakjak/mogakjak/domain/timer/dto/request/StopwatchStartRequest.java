package com.mogakjak.mogakjak.domain.timer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StopwatchStartRequest(

        @NotNull
        @Schema(description = "해당 스톱워치 세션의 todoId", example = "7f000001-9a3d-1f34-819a-3d92e3800004")
        UUID todoId

){}
