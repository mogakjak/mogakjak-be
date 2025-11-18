package com.mogakjak.mogakjak.domain.invitation.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "그룹 초대 URL 응답 DTO")
public class InvitationUrlResponse {

    @Schema(description = "그룹 ID")
    private UUID groupId;

    @Schema(description = "초대 URL")
    private String invitationUrl;
}