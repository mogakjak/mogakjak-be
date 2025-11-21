package com.mogakjak.mogakjak.global.websocket.dto;

import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimerCompletionNotificationDto {
    private UUID sessionId;
    private UUID userId;
    private UUID groupId; // 그룹 내 개인 타이머인 경우
    private TimerMode mode; // TIMER, POMODORO
    private String message; // 알림 메시지
    private String todoTitle; // 할 일 제목
}

