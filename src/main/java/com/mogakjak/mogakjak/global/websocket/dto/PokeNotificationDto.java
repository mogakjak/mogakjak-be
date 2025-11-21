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
public class PokeNotificationDto {
    private UUID fromUserId;
    private String fromUserNickname;
    private UUID targetUserId;
    private UUID groupId;
    private String groupName;
    private String message; // "그룹이름에서 같이 모각작해요!!" 등의 메시지
}

