package com.mogakjak.mogakjak.domain.todo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTodoRequest {

    @NotNull
    private UUID categoryId;

    @NotBlank
    @Size(max = 35, message = "할 일은 35자를 초과할 수 없습니다.") // 기능 명세 6.2-4
    private String task;

    @NotNull
    private LocalDate date;

    @Range(min = 60, max = 86400, message = "목표 시간은 1분(60초)에서 24시간(86400초) 사이여야 합니다.") // 기능 명세 6.2-6
    @Schema(description = "할 일의 누적 목표시간(초). 생략 또는 null이면 미설정", example = "3600", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer targetTimeInSeconds;
}
