package com.mogakjak.mogakjak.domain.timer.dto.request;

import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StopwatchStartRequest(

        @NotNull
        @Schema(description = "해당 스톱워치 세션의 todoId", example = "7f000001-9a3d-1f34-819a-3d92e3800004")
        UUID todoId,

        @NotNull
        @Schema(description = "참여 타입 (INDIVIDUAL: 일반 개인 타이머, GROUP: 그룹 내 개인 타이머)", example = "INDIVIDUAL | GROUP")
        ParticipationType participationType,

        @Schema(description = "그룹 ID (participationType이 GROUP일 때 필수)", example = "7f000001-9a3d-1f34-819a-3d92e3800004")
        UUID groupId

){}
