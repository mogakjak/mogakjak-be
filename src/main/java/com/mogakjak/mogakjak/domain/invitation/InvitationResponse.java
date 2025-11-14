package com.mogakjak.mogakjak.domain.invitation;

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

    public static InvitationResponse from(Invitation invitation) {
        return InvitationResponse.builder()
                .invitationId(invitation.getId())
                .groupId(invitation.getGroup().getId())
                .groupName(invitation.getGroup().getName())
                .inviterNickname(invitation.getInviter().getName())
                .build();
    }
}