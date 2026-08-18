package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.websocket.dto.PokeNotificationDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PokeNotificationService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 콕 찌르기 알림 전송
     */
    @Transactional
    public void sendPokeNotification(UUID fromUserId, UUID targetUserId, UUID groupId) {
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 알림 메시지 생성
        String message = String.format("\"%s\"에서 같이 모각작해요!!", group.getName());

        PokeNotificationDto notification = PokeNotificationDto.builder()
                .fromUserId(fromUserId)
                .fromUserNickname(fromUser.getName())
                .targetUserId(targetUserId)
                .groupId(groupId)
                .groupName(group.getName())
                .message(message)
                .build();

        try {
            String jsonMessage = objectMapper.writeValueAsString(notification);
            realtimeEventPublisher.publish("poke-notification", jsonMessage);
            
            log.debug("콕 찌르기 알림 전송: fromUserId={}, targetUserId={}, groupId={}", 
                    fromUserId, targetUserId, groupId);
        } catch (JsonProcessingException e) {
            log.error("콕 찌르기 알림 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send poke notification", e);
        }
    }
}
