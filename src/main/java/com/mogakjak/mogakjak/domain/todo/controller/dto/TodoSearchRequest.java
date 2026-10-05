package com.mogakjak.mogakjak.domain.todo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class TodoSearchRequest {
    @Size(max = 35)
    @Schema(description = "제목 검색어. 초성·혼합 입력·부분 문자열 지원. 빈 값은 전체 목록", example = "ㄱㅂ")
    private String keyword = "";

    @NotNull
    @Min(1)
    @Max(100)
    @Schema(description = "반환 개수(1~100), 기본 20", defaultValue = "20")
    private Integer limit = 20;

    @Schema(description = "이전 응답의 nextCursor. 검색어 변경 시 생략", nullable = true)
    private UUID cursor;
}
