package com.mogakjak.mogakjak.domain.todo.controller.dto;

import com.mogakjak.mogakjak.global.enumerate.CategoryColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryRequest {

    @NotNull
    UUID categoryId;

    @NotBlank
    @Size(max = 20)
    private String name;

    @NotNull
    private CategoryColor color;
}
