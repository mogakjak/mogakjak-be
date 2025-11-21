package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mogakjak.mogakjak.global.websocket.dto.ChatMessageDto;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.GroupMemberStatusUpdateDto;
import com.mogakjak.mogakjak.global.websocket.dto.TimerCompletionNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.PokeNotificationDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
public class RedisPubSubService implements MessageListener {

    private final StringRedisTemplate stringRedisTemplate;
    private final SimpMessageSendingOperations messageTemplate;
    private final ObjectMapper objectMapper;

    public RedisPubSubService(@Qualifier("chatPubSub") StringRedisTemplate stringRedisTemplate, SimpMessageSendingOperations messageTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.messageTemplate = messageTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void publish(String channel, String message){
        stringRedisTemplate.convertAndSend(channel, message);
    }

    @Override
//    pattern에는 topic의 이름의 패턴이 담겨있고, 이 패턴을 기반으로 다이나믹한 코딩
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        String channel = new String(pattern);
        
        try {
            if ("chat".equals(channel)) {
                // 채팅 메시지 처리
                ChatMessageDto chatMessageDto = objectMapper.readValue(payload, ChatMessageDto.class);
                messageTemplate.convertAndSend("/topic/"+chatMessageDto.getRoomId(), chatMessageDto);
            } else if ("focus-notification".equals(channel)) {
                // 집중 체크 알림 처리
                FocusNotificationDto notificationDto = objectMapper.readValue(payload, FocusNotificationDto.class);
                messageTemplate.convertAndSend("/topic/group/"+notificationDto.getGroupId()+"/notification", notificationDto);
            } else if ("group-member-status".equals(channel)) {
                // 그룹 멤버 상태 업데이트 처리
                GroupMemberStatusUpdateDto statusUpdateDto = objectMapper.readValue(payload, GroupMemberStatusUpdateDto.class);
                messageTemplate.convertAndSend("/topic/group/"+statusUpdateDto.getGroupId()+"/member-status", statusUpdateDto);
            } else if ("timer-completion".equals(channel)) {
                // 타이머 완료 알림 처리
                TimerCompletionNotificationDto completionDto = objectMapper.readValue(payload, TimerCompletionNotificationDto.class);
                // 개인 타이머 알림: /topic/user/{userId}/timer-completion
                messageTemplate.convertAndSend("/topic/user/"+completionDto.getUserId()+"/timer-completion", completionDto);
            } else if ("poke-notification".equals(channel)) {
                // 콕 찌르기 알림 처리
                PokeNotificationDto pokeDto = objectMapper.readValue(payload, PokeNotificationDto.class);
                // 개인 알림: /topic/user/{userId}/poke
                messageTemplate.convertAndSend("/topic/user/"+pokeDto.getTargetUserId()+"/poke", pokeDto);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize message from channel: " + channel, e);
        }
    }
}
