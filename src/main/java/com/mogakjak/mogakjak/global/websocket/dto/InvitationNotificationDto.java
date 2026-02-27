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
public class InvitationNotificationDto {
    private UUID invitationId;
    private UUID groupId;
    private String groupName;
    private UUID inviterId;
    private String inviterNickname;
    private UUID inviteeId;
    private String message;
}

