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
public class CheerNotificationDto {
    private UUID fromUserId;
    private String fromUserNickname;
    private UUID targetUserId;
    private UUID groupId;
    private String groupName;
    private String message; // "누구누구 님이 응원을 보냈어요!" 등의 메시지
}

