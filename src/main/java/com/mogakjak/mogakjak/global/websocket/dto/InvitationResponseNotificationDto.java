package com.mogakjak.mogakjak.global.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationResponseNotificationDto {
    private UUID invitationId;
    private UUID groupId;
    private String groupName;
    private UUID inviterId;
    private UUID inviteeId;
    private String inviteeNickname;
    private String status; // "ACCEPTED" or "DECLINED"
    private String message;
}

