package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.global.websocket.dto.TimerCompletionNotificationDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimerCompletionNotificationService {

    private final FocusSessionRepository focusSessionRepository;
    private final FocusIntervalRepository focusIntervalRepository;
    private final TodoRepository todoRepository;
    private final RedisPubSubService redisPubSubService;
    
    // 각 세션의 스케줄된 알림 작업 추적
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> scheduledNotifications = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        // 스레드 풀 크기는 활성 타이머 수에 따라 조정 가능
        this.scheduler = Executors.newScheduledThreadPool(10);
        
        // ObjectMapper 초기화
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 타이머 시작 시 종료 예정 시간에 알림을 스케줄링
     */
    public void scheduleCompletionNotification(UUID sessionId) {
        // 기존 스케줄이 있으면 취소
        cancelScheduledNotification(sessionId);
        
        Optional<FocusSession> focusSessionOpt = focusSessionRepository.findById(sessionId);
        if (focusSessionOpt.isEmpty()) {
            log.warn("타이머 완료 알림 스케줄링 실패: 세션을 찾을 수 없습니다. sessionId={}", sessionId);
            return;
        }

        FocusSession focusSession = focusSessionOpt.get();
        
        // 스톱워치는 종료 예정 시간이 없음
        if (focusSession.getMode() == TimerMode.STOPWATCH) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expectedEndTime = calculateExpectedEndTime(focusSession, now);
        
        if (expectedEndTime == null) {
            log.debug("타이머 완료 알림 스케줄링 스킵: 종료 예정 시간을 계산할 수 없습니다. sessionId={}", sessionId);
            return;
        }

        // 현재 시간보다 과거면 즉시 알림 전송
        if (expectedEndTime.isBefore(now) || expectedEndTime.isEqual(now)) {
            sendCompletionNotification(focusSession);
            return;
        }

        // 종료 예정 시간까지의 지연 시간 계산 (초 단위)
        long delaySeconds = Duration.between(now, expectedEndTime).getSeconds();
        
        // 스케줄링
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                // 스케줄 실행 시점에 다시 세션 조회하여 상태 확인
                Optional<FocusSession> currentSessionOpt = focusSessionRepository.findById(sessionId);
                if (currentSessionOpt.isEmpty()) {
                    log.debug("타이머 완료 알림 실행 스킵: 세션이 삭제되었습니다. sessionId={}", sessionId);
                    scheduledNotifications.remove(sessionId);
                    return;
                }

                FocusSession currentSession = currentSessionOpt.get();
                
                // 이미 종료된 세션은 알림 전송하지 않음
                if (currentSession.getStatus() == TimerStatus.FINISHED) {
                    log.debug("타이머 완료 알림 실행 스킵: 세션이 이미 종료되었습니다. sessionId={}", sessionId);
                    scheduledNotifications.remove(sessionId);
                    return;
                }

                // 알림 전송
                sendCompletionNotification(currentSession);
                scheduledNotifications.remove(sessionId);
            } catch (Exception e) {
                log.error("타이머 완료 알림 실행 중 에러 발생 (sessionId: {}): {}", sessionId, e.getMessage(), e);
                scheduledNotifications.remove(sessionId);
            }
        }, delaySeconds, TimeUnit.SECONDS);

        scheduledNotifications.put(sessionId, future);
        log.debug("타이머 완료 알림 스케줄링 완료: sessionId={}, 예정 시간={}, 지연 시간={}초", 
                sessionId, expectedEndTime, delaySeconds);
    }

    /**
     * pause/resume 시 종료 예정 시간을 재계산하여 알림 스케줄 재설정
     */
    public void rescheduleCompletionNotification(UUID sessionId) {
        scheduleCompletionNotification(sessionId);
    }

    /**
     * 타이머 종료 시 스케줄된 알림 취소
     */
    public void cancelScheduledNotification(UUID sessionId) {
        ScheduledFuture<?> future = scheduledNotifications.remove(sessionId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.debug("타이머 완료 알림 스케줄 취소: sessionId={}", sessionId);
        }
    }

    /**
     * 종료 예정 시간 계산
     * pause된 시간을 고려하여 실제 실행 시간이 targetDuration에 도달했을 때 알림
     */
    private LocalDateTime calculateExpectedEndTime(FocusSession focusSession, LocalDateTime now) {
        if (focusSession.getMode() == TimerMode.STOPWATCH) {
            // 스톱워치는 종료 예정 시간이 없음
            return null;
        }

        LocalDateTime startedAt = focusSession.getStartedAt();
        
        if (focusSession.getMode() == TimerMode.TIMER) {
            // 타이머: targetDuration만큼 실행되어야 함
            if (focusSession.getTargetDuration() == null) {
                return null;
            }
            
            // pause된 시간 계산
            long pausedSeconds = calculatePausedSeconds(focusSession, now);
            
            // 종료 예정 시간 = 시작 시간 + targetDuration + pause된 시간
            // (pause된 시간만큼 종료 시간이 늦춰짐)
            return startedAt.plusSeconds(focusSession.getTargetDuration() + pausedSeconds);
            
        } else if (focusSession.getMode() == TimerMode.POMODORO) {
            // 뽀모도로: 각 focus phase가 끝날 때마다 알림이 필요할 수 있지만,
            // 일단 전체 뽀모도로가 끝날 때 알림을 보내도록 구현
            if (focusSession.getFocusDuration() == null || focusSession.getRepeatCount() == null) {
                return null;
            }
            
            // 전체 뽀모도로 시간 = focusDuration * repeatCount + breakDuration * (repeatCount - 1)
            // 마지막 round는 break가 없으므로 breakDuration * (repeatCount - 1)
            long totalPomodoroSeconds = focusSession.getFocusDuration() * focusSession.getRepeatCount();
            if (focusSession.getBreakDuration() != null && focusSession.getRepeatCount() > 1) {
                totalPomodoroSeconds += focusSession.getBreakDuration() * (focusSession.getRepeatCount() - 1);
            }
            
            // pause된 시간 계산
            long pausedSeconds = calculatePausedSeconds(focusSession, now);
            
            // 종료 예정 시간 = 시작 시간 + 전체 뽀모도로 시간 + pause된 시간
            return startedAt.plusSeconds(totalPomodoroSeconds + pausedSeconds);
        }

        return null;
    }

    /**
     * pause된 시간 계산 (초 단위)
     * FocusInterval들 사이의 gap과 현재 pause 상태를 고려
     */
    private long calculatePausedSeconds(FocusSession focusSession, LocalDateTime now) {
        List<FocusInterval> intervals = focusIntervalRepository.findAllBySessionId(focusSession.getId());
        
        if (intervals.isEmpty()) {
            // interval이 없으면 pause 상태인지만 확인
            if (focusSession.getStatus() == TimerStatus.PAUSED) {
                return Duration.between(focusSession.getStartedAt(), now).getSeconds();
            }
            return 0;
        }
        
        long pausedSeconds = 0;
        LocalDateTime lastEndTime = focusSession.getStartedAt();
        
        // interval들을 시작 시간 순으로 정렬하여 처리
        intervals.sort((a, b) -> a.getStartedAt().compareTo(b.getStartedAt()));
        
        for (FocusInterval interval : intervals) {
            // interval 시작 전까지의 pause 시간 (gap)
            if (interval.getStartedAt().isAfter(lastEndTime)) {
                pausedSeconds += Duration.between(lastEndTime, interval.getStartedAt()).getSeconds();
            }
            
            // interval이 끝났으면 endedAt 사용, 안 끝났으면 현재 시간 사용
            LocalDateTime intervalEnd = interval.getEndedAt() != null ? interval.getEndedAt() : now;
            lastEndTime = intervalEnd;
        }
        
        // 마지막 interval 이후의 pause 시간 (현재 pause 중인 경우)
        if (focusSession.getStatus() == TimerStatus.PAUSED && lastEndTime.isBefore(now)) {
            pausedSeconds += Duration.between(lastEndTime, now).getSeconds();
        }
        
        return pausedSeconds;
    }

    /**
     * 타이머 완료 알림 전송
     */
    @Transactional
    public void sendCompletionNotification(FocusSession focusSession) {
        try {
            String todoTitle = null;
            if (focusSession.getTodoId() != null) {
                Optional<Todo> todoOpt = todoRepository.findById(focusSession.getTodoId());
                if (todoOpt.isPresent()) {
                    todoTitle = todoOpt.get().getTask();
                }
            }

            String message = focusSession.getMode() == TimerMode.POMODORO
                    ? "뽀모도로가 완료되었습니다!"
                    : "타이머가 완료되었습니다!";

            TimerCompletionNotificationDto notification = TimerCompletionNotificationDto.builder()
                    .sessionId(focusSession.getId())
                    .userId(focusSession.getUserId())
                    .groupId(focusSession.getGroupId())
                    .mode(focusSession.getMode())
                    .message(message)
                    .todoTitle(todoTitle)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(notification);
            
            // 개인 타이머 알림 채널
            redisPubSubService.publish("timer-completion", jsonMessage);
            
            log.debug("타이머 완료 알림 전송: sessionId={}, userId={}", 
                    focusSession.getId(), focusSession.getUserId());
        } catch (JsonProcessingException e) {
            log.error("타이머 완료 알림 전송 실패: {}", e.getMessage(), e);
        }
    }
}

