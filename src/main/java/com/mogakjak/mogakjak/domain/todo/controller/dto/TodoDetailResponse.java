package com.mogakjak.mogakjak.domain.todo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

@Schema(description = "본인 소유의 선택한 할 일과 활성 타이머의 읽기 전용 스냅샷")
public record TodoDetailResponse(
        TodoResponse todo,
        CategoryResponse category,
        @Schema(description = "할 일의 저장된 누적시간 + 미저장 현재 집중 구간(초). 휴식 제외", example = "960")
        long accumulatedTimeInSeconds,
        @Schema(description = "표시용 누적시간 기준 달성률(0~100). 목표 미설정이면 null", example = "26", nullable = true)
        Integer progressRate,
        @Schema(description = "활성 세션의 할 일 공개 여부. 활성 세션 없으면 true")
        boolean isTaskPublic,
        @Schema(description = "활성 세션의 누적시간 공개 여부. 활성 세션 없으면 true")
        boolean isTimerPublic,
        @Schema(description = "선택한 할 일의 본인 활성 RUNNING/PAUSED 세션. 없으면 null", nullable = true)
        ActiveTodoSessionResponse activeSession,
        @Schema(description = "스냅샷 기준 시각(+09:00). 조회는 저장하지 않음", example = "2026-10-05T12:34:56+09:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime serverTime
) {}
