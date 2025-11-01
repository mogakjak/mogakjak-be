package com.mogakjak.mogakjak.domain.todo.controller.dto;

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

    @NotNull
    @Range(min = 60, max = 86400)
    private Integer targetTimeInSeconds;
}
