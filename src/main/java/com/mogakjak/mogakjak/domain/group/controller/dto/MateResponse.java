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

    @Schema(description = "현재 상태 메시지")
    private String statusMessage;

    // User 엔티티에서 필요한 정보로 생성 (User 엔티티 구조에 맞게 수정 필요)
    public static MateResponse from(User user) {
        return MateResponse.builder()
                .userId(user.getId())
                .nickname(user.getName()) // User 엔티티에 getName()이 있다고 가정
                // .profileUrl(user.getProfileUrl()) // User 엔티티에 프로필 URL이 있다고 가정
                // .statusMessage(user.getStatusMessage()) // User 엔티티에 상태 메시지가 있다고 가정
                .build();
    }
}