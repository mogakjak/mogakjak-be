package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "그룹 정보 수정 요청 DTO")
public class UpdateGroupRequest {

    private String name;
    private String imageUrl;
}