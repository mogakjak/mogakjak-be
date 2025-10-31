package com.mogakjak.mogakjak.controller.dto;

import com.mogakjak.mogakjak.domain.user.enumerate.CategoryColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {

    @NotBlank
    @Size(max = 20)
    private String name;

    @NotNull
    private CategoryColor color;
}
