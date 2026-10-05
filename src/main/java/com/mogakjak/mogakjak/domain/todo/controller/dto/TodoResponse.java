package com.mogakjak.mogakjak.domain.todo.controller.dto;

import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
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

    public static TodoResponse from(Todo todo) {
        return TodoResponse.builder()
                .id(todo.getId())
                .categoryId(todo.getCategory().getId())
                .task(todo.getTask())
                .date(todo.getDate())
                .targetTimeInSeconds(todo.getTargetTimeInSeconds())
                .actualTimeInSeconds(todo.getActualTimeInSeconds())
                .isCompleted(todo.getIsCompleted())
                .progressRate(todo.calculateProgressRate())
                .build();
    }

}
