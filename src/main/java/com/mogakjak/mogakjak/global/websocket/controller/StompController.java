package com.mogakjak.mogakjak.global.websocket.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.global.websocket.dto.ChatMessageDto;
import com.mogakjak.mogakjak.global.websocket.service.ChatService;
import com.mogakjak.mogakjak.global.websocket.service.RealtimeEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class StompController {

    private final ChatService chatService;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ObjectMapper objectMapper;

    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable UUID roomId, ChatMessageDto chatMessageReqDto) throws JsonProcessingException {
        chatService.saveMessage(roomId, chatMessageReqDto);
        chatMessageReqDto.setRoomId(roomId);
        String message = objectMapper.writeValueAsString(chatMessageReqDto);
        realtimeEventPublisher.publish("chat", message);
    }
}
