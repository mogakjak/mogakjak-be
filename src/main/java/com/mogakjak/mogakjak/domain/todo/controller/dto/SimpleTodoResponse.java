package com.mogakjak.mogakjak.domain.todo.controller.dto;

import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class SimpleTodoResponse {

    @Schema(description = "할 일 Id", example = "7f000001-9a8c-14fc-819a-8c17e9fc0001")
    private UUID id;

    @Schema(description = "할 일 명", example = "모각작 앤 몰딥브")
    private String task;

    @Schema(description = "목표 시간", example = "")
    private Integer targetTimeInSeconds;

    public static SimpleTodoResponse from(Todo todo) {
        return SimpleTodoResponse.builder()
                .id(todo.getId())
                .task(todo.getTask())
                .targetTimeInSeconds(todo.getTargetTimeInSeconds())
                .build();
    }
}