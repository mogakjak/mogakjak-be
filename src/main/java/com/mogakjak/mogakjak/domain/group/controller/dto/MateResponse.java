package com.mogakjak.mogakjak.domain.group.controller.dto;

import com.mogakjak.mogakjak.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
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
    private List<String> groupNames;

    @Schema(description = "활동 중 여부 (웹사이트 접속 중이거나 개인 타이머 실행 중)")
    private Boolean isActive;

    @Schema(description = "마지막 활동 시간")
    private LocalDateTime lastActivityAt;

    public static MateResponse from(User user, List<String> groupNames) {
        return MateResponse.builder()
                .userId(user.getId())
                .nickname(user.getName())
                .profileUrl(user.getImageUrl())
                .groupNames(groupNames)
                .isActive(user.getIsActive() != null ? user.getIsActive() : false)
                .lastActivityAt(user.getLastActivityAt())
                .build();
    }
}