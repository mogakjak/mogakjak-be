package com.mogakjak.mogakjak.global.websocket.dto;

import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupTimerEventDto {
    private UUID groupId;
    private UUID sessionId;
    private TimerEventType eventType; // START, PAUSE, RESUME, FINISH
    private TimerMode mode;
    private TimerStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime endedAt;
    private Long targetDuration;
    private Long totalDuration; // 현재 세션의 총 시간
    private Long accumulatedDuration; // 그룹 누적 시간 (여러 세션을 거쳐 쌓인 시간)
    private Integer progressRate;
    private LocalDateTime serverTime; // 서버 시간 (동기화용)
    
    public enum TimerEventType {
        START, PAUSE, RESUME, FINISH, SYNC
    }
}

