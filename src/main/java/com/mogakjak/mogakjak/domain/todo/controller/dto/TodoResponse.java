package com.mogakjak.mogakjak.domain.todo.controller.dto;

import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TodoResponse {
    private UUID id;
    private UUID categoryId;
    private String task;
    private LocalDate date;
    @Schema(description = "할 일의 누적 목표시간(초). 미설정이면 null", example = "3600", nullable = true)
    private Integer targetTimeInSeconds;
    private Integer actualTimeInSeconds;
    private Boolean isCompleted;
    @Schema(description = "달성률(0~100). 목표시간 미설정이면 null", example = "0", nullable = true)
    private Integer progressRate;

    @Schema(description = "최근 실제 집중 시각. Asia/Seoul(+09:00) 기준이며 작업 이력 없으면 null", example = "2026-10-05T12:34:56+09:00", nullable = true)
    private OffsetDateTime lastWorkedAt;

    public static TodoResponse from(Todo todo) {
        return from(todo, null);
    }

    public static TodoResponse from(Todo todo, LocalDateTime lastWorkedAt) {
        return TodoResponse.builder()
                .id(todo.getId())
                .categoryId(todo.getCategory().getId())
                .task(todo.getTask())
                .date(todo.getDate())
                .targetTimeInSeconds(todo.getTargetTimeInSeconds())
                .actualTimeInSeconds(todo.getActualTimeInSeconds())
                .isCompleted(todo.getIsCompleted())
                .progressRate(todo.calculateProgressRate())
                .lastWorkedAt(lastWorkedAt == null ? null : lastWorkedAt.atZone(ZoneId.systemDefault())
                        .withZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime())
                .build();
    }

}
