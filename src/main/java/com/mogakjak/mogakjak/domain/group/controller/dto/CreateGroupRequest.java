package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "그룹 생성 요청 DTO")
public class CreateGroupRequest {

    private String name;
    private String imageUrl;
}