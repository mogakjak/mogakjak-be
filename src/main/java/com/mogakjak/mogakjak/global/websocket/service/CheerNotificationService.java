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
import com.mogakjak.mogakjak.global.websocket.dto.CheerNotificationDto;
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
public class CheerNotificationService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RedisPubSubService redisPubSubService;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 응원 알림 전송
     */
    @Transactional
    public void sendCheerNotification(UUID fromUserId, UUID targetUserId, UUID groupId) {
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 알림 메시지 생성
        String message = String.format("%s님이 응원을 보냈어요!", fromUser.getName());

        CheerNotificationDto notification = CheerNotificationDto.builder()
                .fromUserId(fromUserId)
                .fromUserNickname(fromUser.getName())
                .targetUserId(targetUserId)
                .groupId(groupId)
                .groupName(group.getName())
                .message(message)
                .build();

        try {
            String jsonMessage = objectMapper.writeValueAsString(notification);
            redisPubSubService.publish("cheer-notification", jsonMessage);
            log.debug("응원 알림 전송: fromUserId={}, targetUserId={}, groupId={}", 
                    fromUserId, targetUserId, groupId);
        } catch (JsonProcessingException e) {
            log.error("응원 알림 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send cheer notification", e);
        }
    }
}

