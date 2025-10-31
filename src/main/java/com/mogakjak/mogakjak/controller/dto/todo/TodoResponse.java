package com.mogakjak.mogakjak.controller.dto.todo;

import com.mogakjak.mogakjak.domain.entity.Todo;
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
    private Integer targetTimeInSeconds;
    private Integer actualTimeInSeconds;
    private Boolean isCompleted;

    public static TodoResponse from(Todo todo) {
        return TodoResponse.builder()
                .id(todo.getId())
                .categoryId(todo.getCategory().getId())
                .task(todo.getTask())
                .date(todo.getDate())
                .targetTimeInSeconds(todo.getTargetTimeInSeconds())
                .actualTimeInSeconds(todo.getActualTimeInSeconds())
                .isCompleted(todo.getIsCompleted())
                .build();
    }
}