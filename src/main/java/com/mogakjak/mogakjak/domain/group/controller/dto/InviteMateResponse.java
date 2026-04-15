package com.mogakjak.mogakjak.domain.group.controller.dto;

import com.mogakjak.mogakjak.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "초대 가능한 메이트 응답 DTO")
public class InviteMateResponse {

    @Schema(description = "유저 ID")
    private UUID userId;

    @Schema(description = "닉네임")
    private String nickname;

    @Schema(description = "프로필 이미지 URL")
    private String profileUrl;

    @Schema(description = "유저 레벨")
    private Integer level;

    @Schema(description = "속한 그룹 이름")
    private List<String> groupNames;

    @Schema(description = "초대 가능 상태", example = "CAN_INVITE")
    private InviteMateStatus inviteStatus;

    public static InviteMateResponse from(User user, Integer level, List<String> groupNames, InviteMateStatus inviteStatus) {
        return InviteMateResponse.builder()
                .userId(user.getId())
                .nickname(user.getName())
                .profileUrl(user.getImageUrl())
                .level(level)
                .groupNames(groupNames)
                .inviteStatus(inviteStatus)
                .build();
    }
}
