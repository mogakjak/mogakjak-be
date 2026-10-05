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
public class UpdateTodoRequest {

    @NotNull
    private UUID categoryId;

    @NotBlank
    @Size(max = 35)
    private String task;

    @NotNull
    private LocalDate date;

    @Range(min = 60, max = 86400)
    @Schema(description = "할 일의 누적 목표시간(초). PUT 요청에서 생략 또는 null이면 기존 목표시간 해제", example = "3600", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer targetTimeInSeconds;
}
