package com.mogakjak.mogakjak.controller.dto.todo;

import com.mogakjak.mogakjak.domain.entity.Category;
import com.mogakjak.mogakjak.domain.enumerate.CategoryColor;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CategoryWithTodosResponse {
    private UUID id;
    private String name;
    private CategoryColor color;
    private Integer displayOrder;
    private Boolean isExpanded;
    private List<TodoResponse> todos;

    public static CategoryWithTodosResponse of(Category category, List<TodoResponse> todos) {
        return CategoryWithTodosResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .displayOrder(category.getDisplayOrder())
                .isExpanded(category.getIsExpanded())
                .todos(todos)
                .build();
    }
}