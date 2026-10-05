package com.mogakjak.mogakjak.domain.timer.dto.request;

import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
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
        @Schema(description = "카운트다운 실행시간(초). 할 일 목표시간과 별개인 필수 실행 설정", example = "1800")
        Long targetSeconds,

        @NotNull
        @Schema(description = "참여 타입 (INDIVIDUAL: 일반 개인 타이머, GROUP: 그룹 내 개인 타이머)", example = "INDIVIDUAL | GROUP")
        ParticipationType participationType,

        @Schema(description = "그룹 ID (participationType이 GROUP일 때 필수)", example = "7f000001-9a3d-1f34-819a-3d92e3800004")
        UUID groupId,

        @Schema(description = "할일 제목 공개 여부 (기본값: true)", example = "true")
        Boolean isTaskPublic,

        @Schema(description = "타이머 누적 시간 공개 여부 (기본값: true)", example = "true")
        Boolean isTimerPublic

) {}
