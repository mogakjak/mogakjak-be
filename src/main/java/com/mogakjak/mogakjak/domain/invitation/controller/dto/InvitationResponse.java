package com.mogakjak.mogakjak.domain.invitation.controller.dto;

import com.mogakjak.mogakjak.domain.invitation.entity.Invitation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@Schema(description = "받은 초대 목록 응답 DTO")
public class InvitationResponse {

    @Schema(description = "초대 ID")
    private UUID invitationId;

    @Schema(description = "그룹 ID")
    private UUID groupId;

    @Schema(description = "그룹 이름")
    private String groupName;

    @Schema(description = "초대한 사람(방장) 닉네임")
    private String inviterNickname;

    @Schema(description = "그룹 이미지 URL")
    private String groupImageUrl;

    @Schema(description = "그룹 활동 중 인원 수 (NOT_PARTICIPATING 제외)")
    private Long activeMemberCount;

    @Schema(description = "그룹 전체 인원 수")
    private Long memberCount;

    public static InvitationResponse from(Invitation invitation, long memberCount, long activeMemberCount) {
        return InvitationResponse.builder()
                .invitationId(invitation.getId())
                .groupId(invitation.getGroup().getId())
                .groupName(invitation.getGroup().getName())
                .inviterNickname(invitation.getInviter().getName())
                .groupImageUrl(invitation.getGroup().getImageUrl())
                .activeMemberCount(activeMemberCount)
                .memberCount(memberCount)
                .build();
    }
}