package com.mogakjak.mogakjak.todo.controller.dto;

import com.mogakjak.mogakjak.user.entity.Category;
import com.mogakjak.mogakjak.global.enumerate.CategoryColor;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CategoryResponse {
    private UUID id;
    private String name;
    private CategoryColor color;
    private Integer displayOrder;
    private Boolean isExpanded;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .displayOrder(category.getDisplayOrder())
                .isExpanded(category.getIsExpanded())
                .build();
    }
}