package com.mogakjak.mogakjak.domain.todo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record TodoSearchResponse(
        List<Item> items,
        boolean hasNext,
        @Schema(description = "다음 요청 cursor에 전달할 마지막 반환 항목 ID. 다음 결과 없으면 null", nullable = true)
        UUID nextCursor
) {
    public record Item(TodoResponse todo, CategoryResponse category) {}
}
