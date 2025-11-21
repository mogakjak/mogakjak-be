package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.group.service.GroupService;
import com.mogakjak.mogakjak.domain.timer.dto.request.GroupTimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerResponse;
import com.mogakjak.mogakjak.domain.timer.entity.*;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.GroupActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.GroupFocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.GroupFocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.websocket.dto.GroupTimerEventDto;
import com.mogakjak.mogakjak.global.websocket.service.GroupTimerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GroupFocusSessionServiceImpl implements GroupFocusSessionService {

    private final GroupFocusSessionRepository groupFocusSessionRepository;
    private final GroupActiveFocusSessionRepository groupActiveFocusSessionRepository;
    private final GroupFocusIntervalRepository groupFocusIntervalRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;
    private final GroupTimerService groupTimerService;
    private final GroupService groupService;

    @Override
    @Transactional
    public TimerResponse startGroupTimer(User user, UUID groupId, GroupTimerStartRequest request) {
        LocalDateTime now = getCurrentTime();

        validateUserAccessToGroup(user, groupId);
        ensureNoActiveSession(groupId);

        GroupFocusSession focusSession = createFocusSession(TimerMode.TIMER, groupId, now, request.targetSeconds(), null, null, null);

        TimerResponse response = startCommon(groupId, now, focusSession, PomodoroPhaseType.NORMAL, 0);
        
        // 그룹 타이머 시작 이벤트 브로드캐스트
        groupTimerService.broadcastTimerEvent(groupId, response, GroupTimerEventDto.TimerEventType.START);
        
        return response;
    }

    @Override
    @Transactional
    public TimerResponse pauseSession(User user, UUID groupId, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        validateUserAccessToGroup(user, groupId);

        getValidatedActiveFocusSession(groupId, sessionId);
        GroupFocusSession currentFocusSession = getValidatedFocusSession(groupId, sessionId);
        GroupFocusInterval currentInterval = getLatestInterval(sessionId);

        validatePauseableState(currentFocusSession);

        currentInterval.end(now);
        long intervalDurationSeconds = calculateIntervalDurationSeconds(currentInterval);

        currentFocusSession.addDuration(intervalDurationSeconds);
        Integer progressRate = calculateProgressRate(currentFocusSession.getTargetDuration(), currentFocusSession.getTotalDuration());
        currentFocusSession.pause(progressRate);

        TimerResponse response = TimerResponse.fromGroupPause(currentFocusSession, now);
        
        // 그룹 타이머 중지 이벤트 브로드캐스트
        groupTimerService.broadcastTimerEvent(groupId, response, GroupTimerEventDto.TimerEventType.PAUSE);
        
        return response;
    }

    @Override
    @Transactional
    public TimerResponse resumeSession(User user, UUID groupId, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        validateUserAccessToGroup(user, groupId);

        getValidatedActiveFocusSession(groupId, sessionId);
        GroupFocusSession currentFocusSession = getValidatedFocusSession(groupId, sessionId);
        GroupFocusInterval latestInterval = getLatestInterval(sessionId);

        validateResumableState(currentFocusSession);

        GroupFocusInterval focusInterval = GroupFocusInterval.create(
                currentFocusSession.getId(),
                now,
                latestInterval.getPhaseType(),
                latestInterval.getRound()
        );
        groupFocusIntervalRepository.save(focusInterval);

        currentFocusSession.resume();

        TimerResponse response = TimerResponse.fromGroupResume(currentFocusSession);
        
        // 그룹 타이머 재개 이벤트 브로드캐스트
        groupTimerService.broadcastTimerEvent(groupId, response, GroupTimerEventDto.TimerEventType.RESUME);
        
        return response;
    }

    @Override
    @Transactional
    public TimerResponse finishSession(User user, UUID groupId, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        validateUserAccessToGroup(user, groupId);

        GroupActiveFocusSession currentActiveSession = getValidatedActiveFocusSession(groupId, sessionId);
        GroupFocusSession currentFocusSession = getValidatedFocusSession(groupId, sessionId);
        GroupFocusInterval currentInterval = getLatestInterval(sessionId);

        validateFinishableState(currentFocusSession);

        long intervalDurationSeconds = 0L;
        if (currentFocusSession.getStatus() != TimerStatus.PAUSED) {
            currentInterval.end(now);
            intervalDurationSeconds = calculateIntervalDurationSeconds(currentInterval);
        }

        groupActiveFocusSessionRepository.deleteById(currentActiveSession.getId());

        currentFocusSession.addDuration(intervalDurationSeconds);
        Integer progressRate = calculateProgressRate(currentFocusSession.getTargetDuration(), currentFocusSession.getTotalDuration());
        currentFocusSession.end(now, progressRate);

        // 그룹 누적 시간에 현재 세션의 총 시간 추가
        Long sessionTotalDuration = currentFocusSession.getTotalDuration();
        if (sessionTotalDuration != null && sessionTotalDuration > 0) {
            groupService.addGroupAccumulatedDuration(groupId, sessionTotalDuration);
        }

        TimerResponse response = TimerResponse.fromGroupFinish(currentFocusSession);
        
        // 그룹 타이머 종료 이벤트 브로드캐스트
        groupTimerService.broadcastTimerEvent(groupId, response, GroupTimerEventDto.TimerEventType.FINISH);
        
        return response;
    }

    private LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    private void validateUserAccessToGroup(User user, UUID groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
        userGroupRepository.findByUser_IdAndGroup_Id(user.getId(), groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
    }

    private void ensureNoActiveSession(UUID groupId) {
        groupActiveFocusSessionRepository.findByGroupId(groupId)
                .ifPresent(active -> {
                    throw new CustomException(ErrorCode.ACTIVE_SESSION_EXISTS);
                });
    }

    private GroupActiveFocusSession getValidatedActiveFocusSession(UUID groupId, UUID sessionId) {
        GroupActiveFocusSession activeSession =groupActiveFocusSessionRepository.findByGroupId(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_SESSION_NOT_FOUND));

        if (!activeSession.getSessionId().equals(sessionId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACTIVE_SESSION);
        }

        return activeSession;
    }

    private GroupFocusSession getValidatedFocusSession(UUID groupId, UUID sessionId) {
        GroupFocusSession focusSession = groupFocusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        if (!focusSession.getGroupId().equals(groupId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_SESSION);
        }

        return focusSession;
    }

    private GroupFocusInterval getLatestInterval(UUID sessionId) {
        return groupFocusIntervalRepository.findTopBySessionIdOrderByStartedAtDesc(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERVAL_NOT_FOUND));
    }

    private Integer calculateProgressRate(Long sessionTargetDuration, Long totalDuration) {
        if (sessionTargetDuration == null || sessionTargetDuration <= 0) {
            throw new CustomException(ErrorCode.INVALID_TARGET_TIME);
        }
        if (totalDuration == null || totalDuration <= 0) {
            return 0;
        }

        double rate = (double) totalDuration / sessionTargetDuration * 100;
        return (int) Math.min(100, Math.floor(rate));
    }

    private void validatePauseableState(GroupFocusSession session) {
        if (session.getStatus() != TimerStatus.RUNNING) {
            if (session.getStatus() == TimerStatus.PAUSED) {
                throw new CustomException(ErrorCode.SESSION_ALREADY_PAUSED);
            }
            if (session.getStatus() == TimerStatus.FINISHED) {
                throw new CustomException(ErrorCode.SESSION_ALREADY_FINISHED);
            }

            throw new CustomException(ErrorCode.SESSION_NOT_RUNNING);
        }
    }

    private void validateResumableState(GroupFocusSession session) {
        if (session.getStatus() != TimerStatus.PAUSED) {
            if (session.getStatus() == TimerStatus.RUNNING) {
                throw new CustomException(ErrorCode.SESSION_ALREADY_RUNNING);
            }
            if (session.getStatus() == TimerStatus.FINISHED) {
                throw new CustomException(ErrorCode.SESSION_ALREADY_FINISHED);
            }

            throw new CustomException(ErrorCode.SESSION_NOT_PAUSED);
        }
    }

    private void validateFinishableState(GroupFocusSession session) {
        TimerStatus status = session.getStatus();

        if (status == TimerStatus.FINISHED) {
            throw new CustomException(ErrorCode.SESSION_ALREADY_FINISHED);
        }

        if (status != TimerStatus.RUNNING && status != TimerStatus.PAUSED) {
            throw new CustomException(ErrorCode.SESSION_NOT_FINISHABLE);
        }
    }

    private long calculateIntervalDurationSeconds(GroupFocusInterval focusInterval) {
        return Duration.between(focusInterval.getStartedAt(), focusInterval.getEndedAt()).getSeconds();
    }

    private GroupFocusSession createFocusSession(TimerMode mode, UUID groupId, LocalDateTime now, Long targetSeconds, Long focusDuration, Long breakDuration, Integer repeatCount) {
        return switch (mode) {
            case TIMER -> GroupFocusSession.createTimerSession(groupId, now, targetSeconds);
//            case STOPWATCH -> GroupFocusSession.createStopwatchSession(groupId, now);
//            case POMODORO -> GroupFocusSession.createPomodoroSession(groupId, now, focusDuration, breakDuration, repeatCount);
            case STOPWATCH -> null;
            case POMODORO -> null;
        };
    }

    private TimerResponse startCommon(UUID groupId, LocalDateTime now, GroupFocusSession focusSession, PomodoroPhaseType phaseType, Integer round) {
        GroupFocusSession savedFocusSession = groupFocusSessionRepository.save(focusSession);

        GroupActiveFocusSession activeSession = GroupActiveFocusSession.create(
                focusSession.getId(),
                groupId,
                now
        );
        groupActiveFocusSessionRepository.save(activeSession);

        GroupFocusInterval focusInterval = GroupFocusInterval.create(
                focusSession.getId(),
                now,
                phaseType,
                round
        );
        groupFocusIntervalRepository.save(focusInterval);

        return TimerResponse.fromGroupStart(savedFocusSession);
    }
}
