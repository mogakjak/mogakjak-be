package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.websocket.dto.UserActiveStatusDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActiveStatusService {

    private final RedisPubSubService redisPubSubService;
    private final UserRepository userRepository;
    // ObjectMapper는 JavaTimeModule을 등록한 상태로 초기화
    private ObjectMapper objectMapper;
    
    @PostConstruct
    public void init() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 사용자의 isActive 상태 변경을 브로드캐스트
     * 모든 메이트 목록을 보는 사용자들에게 실시간으로 전달
     */
    @Transactional(readOnly = true)
    public void broadcastActiveStatusChange(UUID userId, Boolean isActive) {
        try {
            // User 엔티티에서 lastActivityAt 조회
            User user = userRepository.findById(userId)
                    .orElse(null);
            
            UserActiveStatusDto statusDto = UserActiveStatusDto.builder()
                    .userId(userId)
                    .isActive(isActive)
                    .lastActivityAt(user != null ? user.getLastActivityAt() : null)
                    .build();

            String message = objectMapper.writeValueAsString(statusDto);
            log.info("===== 사용자 isActive 상태 브로드캐스트 시작 =====");
            log.info("userId: {}, isActive: {}, lastActivityAt: {}", userId, isActive, statusDto.getLastActivityAt());
            log.info("Redis Pub/Sub 채널: user-active-status");
            log.info("전송할 메시지: {}", message);
            redisPubSubService.publish("user-active-status", message);
            log.info("===== Redis Pub/Sub 전송 완료 =====");
        } catch (JsonProcessingException e) {
            log.error("사용자 isActive 상태 브로드캐스트 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to broadcast user active status", e);
        }
    }
}

