package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerResponse;
import com.mogakjak.mogakjak.domain.timer.entity.GroupFocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.websocket.dto.GroupTimerEventDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupTimerService {

    private final RedisPubSubService redisPubSubService;
    private final GroupRepository groupRepository;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 그룹 타이머 이벤트 브로드캐스트
     */
    @Transactional
    public void broadcastTimerEvent(UUID groupId, TimerResponse timerResponse, GroupTimerEventDto.TimerEventType eventType) {
        try {
            // 그룹의 누적 시간 조회
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
            Long accumulatedDuration = group.getAccumulatedDuration() != null ? group.getAccumulatedDuration() : 0L;
            
            GroupTimerEventDto eventDto = GroupTimerEventDto.builder()
                    .groupId(groupId)
                    .sessionId(timerResponse.sessionId())
                    .eventType(eventType)
                    .mode(timerResponse.mode())
                    .status(timerResponse.status())
                    .startedAt(timerResponse.startedAt())
                    .pausedAt(timerResponse.pausedAt())
                    .endedAt(timerResponse.endedAt())
                    .targetDuration(timerResponse.targetDuration())
                    .totalDuration(timerResponse.totalDuration())
                    .accumulatedDuration(accumulatedDuration)
                    .progressRate(timerResponse.progressRate())
                    .serverTime(LocalDateTime.now())
                    .build();

            String message = objectMapper.writeValueAsString(eventDto);
            redisPubSubService.publish("group-timer-event", message);
            
            log.debug("그룹 타이머 이벤트 브로드캐스트: groupId={}, eventType={}", groupId, eventType);
        } catch (JsonProcessingException e) {
            log.error("그룹 타이머 이벤트 브로드캐스트 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to broadcast group timer event", e);
        }
    }

    /**
     * 그룹 타이머 동기화 메시지 브로드캐스트 (10초마다)
     */
    @Transactional
    public void broadcastTimerSync(UUID groupId, GroupFocusSession session, long calculatedTotalDuration) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // progressRate 재계산
            Integer progressRate = null;
            if (session.getTargetDuration() != null && session.getTargetDuration() > 0 
                    && calculatedTotalDuration > 0) {
                double rate = (double) calculatedTotalDuration / session.getTargetDuration() * 100;
                progressRate = (int) Math.min(100, Math.floor(rate));
            }
            
            // 그룹의 누적 시간 조회
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
            Long accumulatedDuration = group.getAccumulatedDuration() != null ? group.getAccumulatedDuration() : 0L;
            
            GroupTimerEventDto eventDto = GroupTimerEventDto.builder()
                    .groupId(groupId)
                    .sessionId(session.getId())
                    .eventType(GroupTimerEventDto.TimerEventType.SYNC)
                    .mode(session.getMode())
                    .status(session.getStatus())
                    .startedAt(session.getStartedAt())
                    .targetDuration(session.getTargetDuration())
                    .totalDuration(calculatedTotalDuration)
                    .accumulatedDuration(accumulatedDuration)
                    .progressRate(progressRate)
                    .serverTime(now)
                    .build();

            String message = objectMapper.writeValueAsString(eventDto);
            redisPubSubService.publish("group-timer-event", message);
            
            log.debug("그룹 타이머 동기화 브로드캐스트: groupId={}", groupId);
        } catch (JsonProcessingException e) {
            log.error("그룹 타이머 동기화 브로드캐스트 실패: {}", e.getMessage(), e);
        }
    }

}

