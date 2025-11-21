package com.mogakjak.mogakjak.global.websocket.scheduler;

import com.mogakjak.mogakjak.domain.timer.entity.GroupActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.GroupFocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.GroupFocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.GroupActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.GroupFocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.GroupFocusSessionRepository;
import com.mogakjak.mogakjak.global.websocket.service.GroupTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 그룹 타이머 동기화 스케줄러
 * 10초마다 활성 그룹 타이머의 서버 시간을 브로드캐스트하여 프론트엔드와 동기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupTimerSyncScheduler {

    private final GroupActiveFocusSessionRepository groupActiveFocusSessionRepository;
    private final GroupFocusSessionRepository groupFocusSessionRepository;
    private final GroupFocusIntervalRepository groupFocusIntervalRepository;
    private final GroupTimerService groupTimerService;

    /**
     * 10초마다 실행하여 활성 그룹 타이머의 서버 시간을 브로드캐스트
     */
    @Scheduled(fixedRate = 10000) // 10초 = 10000ms
    @Transactional(readOnly = true)
    public void syncGroupTimers() {
        log.debug("그룹 타이머 동기화 스케줄러 실행");
        
        // 모든 활성 그룹 타이머 조회
        List<GroupActiveFocusSession> activeSessions = groupActiveFocusSessionRepository.findAll();

        for (GroupActiveFocusSession activeSession : activeSessions) {
            try {
                Optional<GroupFocusSession> sessionOpt = groupFocusSessionRepository.findById(activeSession.getSessionId());
                
                if (sessionOpt.isEmpty()) {
                    continue;
                }
                
                GroupFocusSession session = sessionOpt.get();
                
                // RUNNING 상태인 경우에만 동기화 (PAUSED는 이벤트로 처리)
                if (session.getStatus() == TimerStatus.RUNNING) {
                    // 현재 interval의 경과 시간 계산
                    Optional<GroupFocusInterval> currentIntervalOpt = 
                            groupFocusIntervalRepository.findTopBySessionIdOrderByStartedAtDesc(session.getId());
                    
                    LocalDateTime now = LocalDateTime.now();
                    long totalSeconds = session.getTotalDuration() != null ? session.getTotalDuration() : 0L;
                    
                    if (currentIntervalOpt.isPresent()) {
                        GroupFocusInterval currentInterval = currentIntervalOpt.get();
                        if (currentInterval.getEndedAt() == null) {
                            // 현재 실행 중인 interval
                            long intervalSeconds = Duration.between(currentInterval.getStartedAt(), now).getSeconds();
                            totalSeconds += intervalSeconds;
                        }
                    }
                    
                    // totalDuration을 임시로 업데이트한 세션 생성 (동기화용으로만 사용)
                    // GroupFocusSession은 protected 생성자이므로 리플렉션이나 별도 메서드 필요
                    // 여기서는 직접 totalDuration을 계산하여 전달
                    GroupFocusSession syncSession = session;
                    
                    // totalDuration을 계산하여 전달하기 위해 별도 메서드 사용
                    groupTimerService.broadcastTimerSync(activeSession.getGroupId(), syncSession, totalSeconds);
                }
            } catch (Exception e) {
                log.error("그룹 {} 타이머 동기화 실패: {}", activeSession.getGroupId(), e.getMessage());
            }
        }
    }
}

