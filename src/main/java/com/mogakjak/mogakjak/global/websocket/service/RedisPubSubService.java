package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mogakjak.mogakjak.global.websocket.dto.ChatMessageDto;
import com.mogakjak.mogakjak.global.websocket.dto.CheerNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationPublishDto;
import com.mogakjak.mogakjak.global.websocket.dto.GroupMemberStatusUpdateDto;
import com.mogakjak.mogakjak.global.websocket.dto.GroupTimerEventDto;
import com.mogakjak.mogakjak.global.websocket.dto.TimerCompletionNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.PokeNotificationDto;
import com.mogakjak.mogakjak.global.websocket.dto.UserActiveStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
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
                // 집중 체크 알림 처리 - 참여 중인 유저에게만 유저별 토픽으로 전송
                FocusNotificationPublishDto publishDto = objectMapper.readValue(payload, FocusNotificationPublishDto.class);
                FocusNotificationDto notificationDto = publishDto.getNotification();
                if (publishDto.getRecipientUserIds() != null) {
                    for (var userId : publishDto.getRecipientUserIds()) {
                        String destination = "/topic/user/" + userId + "/focus-notification";
                        log.info("WebSocket 전송: {} -> {}", destination, notificationDto);
                        messageTemplate.convertAndSend(destination, notificationDto);
                    }
                }
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
            } else if ("cheer-notification".equals(channel)) {
                // 응원 알림 처리
                CheerNotificationDto cheerDto = objectMapper.readValue(payload, CheerNotificationDto.class);
                // 개인 알림: /topic/user/{userId}/cheer
                messageTemplate.convertAndSend("/topic/user/"+cheerDto.getTargetUserId()+"/cheer", cheerDto);
            } else if ("group-timer-event".equals(channel)) {
                // 그룹 타이머 이벤트 처리
                GroupTimerEventDto timerEventDto = objectMapper.readValue(payload, GroupTimerEventDto.class);
                // 그룹 알림: /topic/group/{groupId}/timer
                messageTemplate.convertAndSend("/topic/group/"+timerEventDto.getGroupId()+"/timer", timerEventDto);
            } else if ("user-active-status".equals(channel)) {
                // 사용자 isActive 상태 변경 처리
                log.info("===== Redis Pub/Sub에서 user-active-status 메시지 수신 =====");
                log.info("원본 payload: {}", payload);
                UserActiveStatusDto statusDto = objectMapper.readValue(payload, UserActiveStatusDto.class);
                log.info("파싱된 DTO: userId={}, isActive={}", statusDto.getUserId(), statusDto.getIsActive());
                // 모든 메이트 목록을 보는 사용자들에게 브로드캐스트: /topic/mates/active-status
                log.info("WebSocket 브로드캐스트 시작: /topic/mates/active-status");
                messageTemplate.convertAndSend("/topic/mates/active-status", statusDto);
                log.info("===== WebSocket 브로드캐스트 완료 =====");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize message from channel: " + channel, e);
        }
    }
}
