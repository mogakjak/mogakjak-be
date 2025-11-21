package com.mogakjak.mogakjak.domain.timer.dto.request;

import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PomodoroStartRequest(

        @NotNull
        @Schema(description = "해당 타이머 세션의 todo Id", example = "7f000001-9a3d-1f34-819a-3d92e3800004")
        UUID todoId,

        @NotNull
        @Schema(description = "집중 시간(초 단위)", example = "1200")
        Long focusSeconds,

        @NotNull
        @Schema(description = "휴식 시간(초 단위)", example = "600")
        Long breakSeconds,

        @NotNull
        @Schema(description = "반복 횟수", example = "3")
        Integer repeatCount,

        @NotNull
        @Schema(description = "참여 타입 (INDIVIDUAL: 일반 개인 타이머, GROUP: 그룹 내 개인 타이머)", example = "INDIVIDUAL | GROUP")
        ParticipationType participationType,

        @Schema(description = "그룹 ID (participationType이 GROUP일 때 필수)", example = "7f000001-9a3d-1f34-819a-3d92e3800004")
        UUID groupId

) {}
