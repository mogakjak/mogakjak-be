package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mogakjak.mogakjak.global.websocket.dto.UserActiveStatusDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActiveStatusService {

    private final RedisPubSubService redisPubSubService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 사용자의 isActive 상태 변경을 브로드캐스트
     * 모든 메이트 목록을 보는 사용자들에게 실시간으로 전달
     */
    @Transactional
    public void broadcastActiveStatusChange(java.util.UUID userId, Boolean isActive) {
        try {
            UserActiveStatusDto statusDto = UserActiveStatusDto.builder()
                    .userId(userId)
                    .isActive(isActive)
                    .build();

            String message = objectMapper.writeValueAsString(statusDto);
            log.info("===== 사용자 isActive 상태 브로드캐스트 시작 =====");
            log.info("userId: {}, isActive: {}", userId, isActive);
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

