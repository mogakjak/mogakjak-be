package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "그룹 이름 응답 DTO")
public class GroupNameResponse {
    @Schema(description = "그룹 이름", example = "모각작 그룹")
    private String groupName;
}
