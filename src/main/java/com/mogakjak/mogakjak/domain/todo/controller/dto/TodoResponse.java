package com.mogakjak.mogakjak.domain.todo.controller.dto;

import com.mogakjak.mogakjak.domain.todo.entity.Todo;
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
    private Integer achievementRate;

    public static TodoResponse from(Todo todo) {
        Integer achievementRate = calculateAchievementRate(
                todo.getActualTimeInSeconds(),
                todo.getTargetTimeInSeconds()
        );

        return TodoResponse.builder()
                .id(todo.getId())
                .categoryId(todo.getCategory().getId())
                .task(todo.getTask())
                .date(todo.getDate())
                .targetTimeInSeconds(todo.getTargetTimeInSeconds())
                .actualTimeInSeconds(todo.getActualTimeInSeconds())
                .isCompleted(todo.getIsCompleted())
                .achievementRate(achievementRate)
                .build();
    }

    private static Integer calculateAchievementRate(Integer actualTimeInSeconds, Integer targetTimeInSeconds) {
        if (targetTimeInSeconds == null || targetTimeInSeconds <= 0) {
            return 0;
        }
        if (actualTimeInSeconds == null || actualTimeInSeconds <= 0) {
            return 0;
        }

        double rate = (double) actualTimeInSeconds / targetTimeInSeconds * 100;
        return (int) Math.min(100, Math.floor(rate));
    }
}