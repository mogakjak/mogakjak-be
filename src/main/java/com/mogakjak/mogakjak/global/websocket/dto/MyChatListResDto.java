package com.mogakjak.mogakjak.global.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MyChatListResDto {
    private UUID roomId;
    private String roomName;
    private String isGroupChat;
    private Long unReadCount;
}
