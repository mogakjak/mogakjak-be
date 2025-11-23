package com.mogakjak.mogakjak.domain.group.controller.dto;

import com.mogakjak.mogakjak.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@Schema(description = "내 모각작 메이트 정보 응답 DTO")
public class MateResponse {

    @Schema(description = "유저 ID")
    private UUID userId;

    @Schema(description = "닉네임")
    private String nickname;

    @Schema(description = "프로필 이미지 URL")
    private String profileUrl;

    @Schema(description = "속한 그룹 이름")
    private String groupName;

    @Schema(description = "활동 중 여부 (웹사이트 접속 중이거나 개인 타이머 실행 중)")
    private Boolean isActive;

    public static MateResponse from(User user, String groupName) {
        return MateResponse.builder()
                .userId(user.getId())
                .nickname(user.getName())
                .profileUrl(user.getImageUrl())
                .groupName(groupName)
                .isActive(user.getIsActive() != null ? user.getIsActive() : false)
                .build();
    }
}