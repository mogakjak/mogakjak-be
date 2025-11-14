package com.mogakjak.mogakjak.domain.group.controller.dto;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "그룹 상세 정보 응답 DTO")
public class GroupDetailResponse {

    @Schema(description = "그룹 ID")
    private UUID groupId;

    @Schema(description = "그룹 이름")
    private String name;

    @Schema(description = "그룹 설명")
    private String description;

    @Schema(description = "비밀번호 존재 여부 (공개/비공개)")
    private boolean isPrivate;

    @Schema(description = "그룹 멤버 목록")
    private List<MemberInfo> members;

    @Getter
    @Builder
    @Schema(description = "그룹 멤버 정보")
    public static class MemberInfo {
        @Schema(description = "유저 ID")
        private UUID userId;
        @Schema(description = "유저 닉네임")
        private String nickname;
        @Schema(description = "유저 프로필 이미지 URL")
        private String profileUrl;
        @Schema(description = "그룹 내 역할 (HOST, MEMBER)")
        private String role;
    }

    public static GroupDetailResponse from(Group group, List<MemberInfo> members) {
        return GroupDetailResponse.builder()
                .groupId(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .isPrivate(group.getPassword() != null && !group.getPassword().isEmpty())
                .members(members)
                .build();
    }
}