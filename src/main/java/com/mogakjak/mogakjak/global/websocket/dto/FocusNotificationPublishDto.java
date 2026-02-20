package com.mogakjak.mogakjak.global.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Redis Pub/Sub로 발행할 집중 체크 알림 payload.
 * - 서버 내부 전용(수신자 필터링 목적)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FocusNotificationPublishDto {
    private FocusNotificationDto notification;
    private List<UUID> recipientUserIds;
}

