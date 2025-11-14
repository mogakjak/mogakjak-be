package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "그룹 정보 수정 요청 DTO")
public class UpdateGroupRequest {

    @Size(max = 30, message = "그룹 이름은 30자를 초과할 수 없습니다.")
    @Schema(description = "새 그룹 이름", example = "큐시즘 29기 스프링 (수정)")
    private String name;

    @Size(max = 100, message = "그룹 설명은 100자를 초과할 수 없습니다.")
    @Schema(description = "새 그룹 설명", example = "수정된 스프링 스터디 그룹입니다.")
    private String description;

    @Schema(description = "새 비밀번호 (null 또는 빈 문자열 시 비밀번호 유지/변경 없음)", example = "5678")
    private String password;
}