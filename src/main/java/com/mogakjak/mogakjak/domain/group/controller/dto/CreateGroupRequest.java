package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "그룹 생성 요청 DTO")
public class CreateGroupRequest {

    @NotBlank(message = "그룹 이름은 필수입니다.")
    @Size(max = 30, message = "그룹 이름은 30자를 초과할 수 없습니다.")
    @Schema(description = "그룹 이름", example = "큐시즘 29기 스프링")
    private String name;

    @Size(max = 100, message = "그룹 설명은 100자를 초과할 수 없습니다.")
    @Schema(description = "그룹 설명", example = "스프링 스터디 그룹입니다.")
    private String description;
}