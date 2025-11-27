package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.group.service.GroupService;
import com.mogakjak.mogakjak.domain.timer.dto.request.PomodoroStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.request.StopwatchStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerResponse;
import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.timer.enumerate.ParticipationType;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.websocket.service.GroupMemberStatusService;
import com.mogakjak.mogakjak.global.websocket.service.TimerCompletionNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.UserActiveStatusService;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusSessionServiceImpl implements FocusSessionService {

    private final FocusSessionRepository focusSessionRepository;
    private final FocusIntervalRepository focusIntervalRepository;
    private final ActiveFocusSessionRepository activeFocusSessionRepository;
    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberStatusService groupMemberStatusService;
    private final TimerCompletionNotificationService timerCompletionNotificationService;
    private final UserActiveStatusService userActiveStatusService;
    private final GroupService groupService;

    @Override
    @Transactional
    public TimerResponse startTimer(User user, TimerStartRequest request) {
        LocalDateTime now = getCurrentTime();

        ensureNoActiveSession(user.getId());
        Todo todo = getValidatedTodo(user.getId(), request.todoId());

        // ParticipationType.GROUP일 때 groupId 검증
        if (request.participationType() == ParticipationType.GROUP && request.groupId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        FocusSession focusSession = createFocusSession(TimerMode.TIMER, user, todo, now, request.targetSeconds(), null, null, null, request.participationType(), request.groupId(), request.isTaskPublic(), request.isTimerPublic());

        return startCommon(user.getId(), request.groupId(), now, focusSession, todo, PomodoroPhaseType.NORMAL, 0, request.participationType());
    }

    @Override
    @Transactional
    public TimerResponse startStopwatch(User user, StopwatchStartRequest request) {
        LocalDateTime now = getCurrentTime();

        ensureNoActiveSession(user.getId());
        Todo todo = getValidatedTodo(user.getId(), request.todoId());

        // ParticipationType.GROUP일 때 groupId 검증
        if (request.participationType() == ParticipationType.GROUP && request.groupId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        FocusSession focusSession = createFocusSession(TimerMode.STOPWATCH, user, todo, now, null, null, null, null, request.participationType(), request.groupId(), request.isTaskPublic(), request.isTimerPublic());

        return startCommon(user.getId(), request.groupId(), now, focusSession, todo, PomodoroPhaseType.NORMAL, 0, request.participationType());
    }

    @Override
    @Transactional
    public TimerResponse startPomodoro(User user, PomodoroStartRequest request) {
        LocalDateTime now = getCurrentTime();

        ensureNoActiveSession(user.getId());
        Todo todo = getValidatedTodo(user.getId(), request.todoId());

        // ParticipationType.GROUP일 때 groupId 검증
        if (request.participationType() == ParticipationType.GROUP && request.groupId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        FocusSession focusSession = createFocusSession(TimerMode.POMODORO, user, todo, now, null, request.focusSeconds(), request.breakSeconds(), request.repeatCount(), request.participationType(), request.groupId(), request.isTaskPublic(), request.isTimerPublic());

        return startCommon(user.getId(), request.groupId(), now, focusSession, todo, PomodoroPhaseType.FOCUS, 1, request.participationType());
    }

    @Override
    @Transactional
    public TimerResponse pauseSession(User user, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        getValidatedActiveFocusSession(user.getId(), sessionId);
        FocusSession currentFocusSession = getValidatedFocusSession(user.getId(), sessionId);
        FocusInterval currentInterval = getLatestInterval(sessionId);

        Todo todo = getValidatedTodo(user.getId(), currentFocusSession.getTodoId());

        validatePauseableState(currentFocusSession);

        currentInterval.end(now);
        long intervalDurationSeconds = calculateIntervalDurationSeconds(currentInterval);

        currentFocusSession.addDuration(intervalDurationSeconds);
        
        // Todo의 actualTimeInSeconds 업데이트
        Long totalDuration = currentFocusSession.getTotalDuration();
        if (totalDuration != null && totalDuration > 0) {
            todo.updateActualTime(totalDuration.intValue());
            todoRepository.save(todo);
        }
        
        Integer progressRate = calculateProgressRate(todo.getTargetTimeInSeconds(), currentFocusSession.getTotalDuration());
        currentFocusSession.pause(progressRate);

        // pause 시 종료 예정 시간 재계산하여 알림 스케줄 재설정 (실패해도 기존 로직에는 영향 없음)
        try {
            timerCompletionNotificationService.rescheduleCompletionNotification(sessionId);
        } catch (Exception e) {
            log.warn("타이머 완료 알림 스케줄 재설정 실패 (sessionId: {}): {}", sessionId, e.getMessage());
        }

        return TimerResponse.fromPause(currentFocusSession, now);
    }

    @Override
    @Transactional
    public TimerResponse resumeSession(User user, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        getValidatedActiveFocusSession(user.getId(), sessionId);
        FocusSession currentFocusSession = getValidatedFocusSession(user.getId(), sessionId);
        FocusInterval latestInterval = getLatestInterval(sessionId);

        validateResumableState(currentFocusSession);

        FocusInterval focusInterval = FocusInterval.create(
                currentFocusSession.getId(),
                now,
                latestInterval.getPhaseType(),
                latestInterval.getRound()
        );
        focusIntervalRepository.save(focusInterval);

        currentFocusSession.resume();

        // resume 시 종료 예정 시간 재계산하여 알림 스케줄 재설정 (실패해도 기존 로직에는 영향 없음)
        try {
            timerCompletionNotificationService.rescheduleCompletionNotification(sessionId);
        } catch (Exception e) {
            log.warn("타이머 완료 알림 스케줄 재설정 실패 (sessionId: {}): {}", sessionId, e.getMessage());
        }

        return TimerResponse.fromResume(currentFocusSession);
    }

    @Override
    @Transactional
    public TimerResponse finishSession(User user, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        ActiveFocusSession currentActiveSession = getValidatedActiveFocusSession(user.getId(), sessionId);
        FocusSession currentFocusSession = getValidatedFocusSession(user.getId(), sessionId);
        FocusInterval currentInterval = getLatestInterval(sessionId);

        Todo todo = getValidatedTodo(user.getId(), currentFocusSession.getTodoId());

        validateFinishableState(currentFocusSession);

        long intervalDurationSeconds = 0L;
        if (currentFocusSession.getStatus() != TimerStatus.PAUSED) {
            currentInterval.end(now);
            intervalDurationSeconds = calculateIntervalDurationSeconds(currentInterval);
        }

        activeFocusSessionRepository.deleteById(currentActiveSession.getId());

        // 타이머 종료 시 스케줄된 알림 취소 (실패해도 기존 로직에는 영향 없음)
        try {
            timerCompletionNotificationService.cancelScheduledNotification(sessionId);
        } catch (Exception e) {
            log.warn("타이머 완료 알림 스케줄 취소 실패 (sessionId: {}): {}", sessionId, e.getMessage());
        }

        // 개인 타이머 종료 시 isActive 업데이트
        // 다른 활성 세션이 있는지 확인 (예: 다른 타이머가 실행 중일 수 있음)
        boolean hasOtherActiveSession = activeFocusSessionRepository.findByUserId(user.getId()).isPresent();
        boolean wasActive = user.getIsActive();
        log.info("타이머 종료 (finishSession): userId={}, wasActive={}, hasOtherActiveSession={}", user.getId(), wasActive, hasOtherActiveSession);
        if (!hasOtherActiveSession) {
            user.setActive(false);
            userRepository.save(user);
            log.info("타이머 종료: isActive를 false로 설정: userId={}", user.getId());
            
            // 상태가 변경되었으면 브로드캐스트 (트랜잭션 커밋 후)
            if (wasActive) {
                log.info("타이머 종료: isActive 상태 변경 감지, 브로드캐스트 예약: userId={}, isActive=false", user.getId());
                TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            log.info("타이머 종료: 트랜잭션 커밋 완료, 브로드캐스트 실행: userId={}, isActive=false", user.getId());
                            userActiveStatusService.broadcastActiveStatusChange(user.getId(), false);
                        }
                    }
                );
            } else {
                log.info("타이머 종료: isActive 상태 변경 없음 (이미 false), 브로드캐스트 안 함: userId={}", user.getId());
            }

                // 그룹 내 개인 타이머인 경우 해당 그룹의 참여 상태를 RESTING으로 변경
                if (currentFocusSession.getParticipationType() == ParticipationType.GROUP && currentFocusSession.getGroupId() != null) {
                    UserGroup userGroup = userGroupRepository.findByUser_IdAndGroup_Id(user.getId(), currentFocusSession.getGroupId())
                            .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
                    
                    // 다른 활성 세션이 없고 그룹 세션에 참여 중인 상태면 RESTING으로 변경
                    if (userGroup.getParticipationStatus() != GroupParticipationStatus.NOT_PARTICIPATING) {
                        userGroup.setParticipationStatus(GroupParticipationStatus.RESTING);
                        userGroupRepository.save(userGroup);
                        
                        // 그룹 멤버 상태 변경 브로드캐스트
                        groupMemberStatusService.broadcastMemberStatusUpdate(currentFocusSession.getGroupId(), user.getId());
                    }
                }
        }

        currentFocusSession.addDuration(intervalDurationSeconds);
        
        // Todo의 actualTimeInSeconds 업데이트
        Long totalDuration = currentFocusSession.getTotalDuration();
        if (totalDuration != null && totalDuration > 0) {
            todo.updateActualTime(totalDuration.intValue());
            todoRepository.save(todo);
        }
        
        Integer progressRate = calculateProgressRate(todo.getTargetTimeInSeconds(), currentFocusSession.getTotalDuration());
        currentFocusSession.end(now, progressRate);

        return TimerResponse.fromFinish(currentFocusSession);
    }

    @Override
    @Transactional
    public TimerResponse nextPomodoroPhase(User user, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        // 유효성 검사
        ActiveFocusSession currentActiveSession = getValidatedActiveFocusSession(user.getId(), sessionId);
        FocusSession focusSession = getValidatedFocusSession(user.getId(), sessionId);
        if (focusSession.getMode() != TimerMode.POMODORO) {
            throw new CustomException(ErrorCode.INVALID_POMODORO_SESSION);
        }

        FocusInterval latestInterval = getLatestInterval(sessionId);
        PomodoroPhaseType currentPhase = latestInterval.getPhaseType();
        Integer currentRound = latestInterval.getRound();

        List<FocusInterval> intervals = focusIntervalRepository.findAllBySessionId(sessionId);
        long accumulatedSeconds = calculateAccumulatedPhaseSeconds(intervals, currentPhase, currentRound, now);
        if (!isPhaseFinished(focusSession, currentPhase, accumulatedSeconds)) {
            throw new CustomException(ErrorCode.PHASE_NOT_FINISHED);
        }

        // 다음 단계로 전환
        if (focusSession.getStatus() != TimerStatus.PAUSED) latestInterval.end(now);
        focusSession.addDuration(accumulatedSeconds);

        if (currentPhase == PomodoroPhaseType.FOCUS && isPomodoroFinished(focusSession, intervals)) {
            activeFocusSessionRepository.deleteById(currentActiveSession.getId());

            // Todo의 actualTimeInSeconds 업데이트
            Todo todo = getValidatedTodo(user.getId(), focusSession.getTodoId());
            Long totalDuration = focusSession.getTotalDuration();
            if (totalDuration != null && totalDuration > 0) {
                todo.updateActualTime(totalDuration.intValue());
                todoRepository.save(todo);
            }
            
            focusSession.end(now, 100);

            // 타이머 종료 시 스케줄된 알림 취소 (실패해도 기존 로직에는 영향 없음)
            try {
                timerCompletionNotificationService.cancelScheduledNotification(sessionId);
            } catch (Exception e) {
                log.warn("타이머 완료 알림 스케줄 취소 실패 (sessionId: {}): {}", sessionId, e.getMessage());
            }

            // 개인 타이머 종료 시 isActive 업데이트
            boolean hasOtherActiveSession = activeFocusSessionRepository.findByUserId(user.getId()).isPresent();
            boolean wasActive = user.getIsActive();
            log.info("타이머 종료 (nextPomodoroPhase): userId={}, wasActive={}, hasOtherActiveSession={}", user.getId(), wasActive, hasOtherActiveSession);
            if (!hasOtherActiveSession) {
                user.setActive(false);
                userRepository.save(user);
                log.info("타이머 종료: isActive를 false로 설정: userId={}", user.getId());
                
                // 상태가 변경되었으면 브로드캐스트 (트랜잭션 커밋 후)
                if (wasActive) {
                    log.info("타이머 종료: isActive 상태 변경 감지, 브로드캐스트 예약: userId={}, isActive=false", user.getId());
                    TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronizationAdapter() {
                            @Override
                            public void afterCommit() {
                                log.info("타이머 종료: 트랜잭션 커밋 완료, 브로드캐스트 실행: userId={}, isActive=false", user.getId());
                                userActiveStatusService.broadcastActiveStatusChange(user.getId(), false);
                            }
                        }
                    );
                } else {
                    log.info("타이머 종료: isActive 상태 변경 없음 (이미 false), 브로드캐스트 안 함: userId={}", user.getId());
                }

                // 그룹 내 개인 타이머인 경우 해당 그룹의 참여 상태를 RESTING으로 변경
                if (focusSession.getParticipationType() == ParticipationType.GROUP && focusSession.getGroupId() != null) {
                    UserGroup userGroup = userGroupRepository.findByUser_IdAndGroup_Id(user.getId(), focusSession.getGroupId())
                            .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
                    
                    // 다른 활성 세션이 없고 그룹 세션에 참여 중인 상태면 RESTING으로 변경
                    if (userGroup.getParticipationStatus() != GroupParticipationStatus.NOT_PARTICIPATING) {
                        userGroup.setParticipationStatus(GroupParticipationStatus.RESTING);
                        userGroupRepository.save(userGroup);
                        
                        // 그룹 멤버 상태 변경 브로드캐스트
                        groupMemberStatusService.broadcastMemberStatusUpdate(focusSession.getGroupId(), user.getId());
                        
                        // 모든 멤버가 NOT_PARTICIPATING이 되면 응원 수 초기화
                        groupService.resetAllCheerCounts(focusSession.getGroupId());
                    }
                }
            }
            
            return TimerResponse.fromFinish(focusSession);
        }

        PomodoroPhaseType nextPhase = nextPhase(currentPhase);
        int nextRound = nextPhase == PomodoroPhaseType.FOCUS ? currentRound + 1 : currentRound;

        FocusInterval nextPhaseInterval = startPhaseInterval(focusSession, nextPhase, now, nextRound);

        return TimerResponse.fromPomodoroPhaseChange(focusSession, nextPhaseInterval);
    }

    @Override
    @Transactional
    public void updatePersonalTimerVisibility(User user, UUID sessionId, Boolean isTaskPublic, Boolean isTimerPublic) {
        FocusSession focusSession = getValidatedFocusSession(user.getId(), sessionId);
        
        // 공개/비공개 설정 업데이트
        if (isTaskPublic != null) {
            focusSession.updateTaskVisibility(isTaskPublic);
        }
        if (isTimerPublic != null) {
            focusSession.updateTimerVisibility(isTimerPublic);
        }
        focusSessionRepository.save(focusSession);
        
        // 그룹 내 개인 타이머인 경우 그룹 멤버 상태 브로드캐스트
        if (focusSession.getParticipationType() == ParticipationType.GROUP && focusSession.getGroupId() != null) {
            UUID groupId = focusSession.getGroupId();
            UUID userId = user.getId();
            log.info("개인 타이머 공개/비공개 설정 변경 - sessionId: {}, isTaskPublic: {}, isTimerPublic: {}, groupId: {}", 
                    sessionId, focusSession.getIsTaskPublic(), focusSession.getIsTimerPublic(), groupId);
            
            // 트랜잭션 커밋 후 브로드캐스트를 위해 TransactionSynchronizationManager 사용
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        log.info("트랜잭션 커밋 완료, 그룹 멤버 상태 브로드캐스트 시작 - groupId: {}, userId: {}", groupId, userId);
                        groupMemberStatusService.broadcastMemberStatusUpdate(groupId, userId);
                    }
                }
            );
        }
    }

    @Override
    @Transactional
    public TimerResponse finishActiveSession(User user) {
        ActiveFocusSession activeSession = activeFocusSessionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_SESSION_NOT_FOUND));

        return finishSession(user, activeSession.getSessionId());
    }

    private Integer calculateProgressRate(Integer todoTargetDuration, Long totalDuration) {
        if (todoTargetDuration == null || todoTargetDuration <= 0) {
            throw new CustomException(ErrorCode.INVALID_TARGET_TIME);
        }
        if (totalDuration == null || totalDuration <= 0) {
            return 0;
        }

        double rate = (double) totalDuration / todoTargetDuration * 100;
        return (int) Math.min(100, Math.floor(rate));
    }

    private Integer calculateProgressRateFromTodo(Todo todo) {
        Integer targetTime = todo.getTargetTimeInSeconds();
        Integer actualTime = todo.getActualTimeInSeconds();

        if (targetTime == null || targetTime <= 0) {
            return 0;
        }
        if (actualTime == null || actualTime <= 0) {
            return 0;
        }

        double rate = (double) actualTime / targetTime * 100;
        return (int) Math.min(100, Math.floor(rate));
    }

    private FocusSession createFocusSession(TimerMode mode, User user, Todo todo, LocalDateTime now, Long targetSeconds, Long focusDuration, Long breakDuration, Integer repeatCount, ParticipationType participationType, UUID groupId, Boolean isTaskPublic, Boolean isTimerPublic) {
        return switch (mode) {
            case TIMER -> FocusSession.createTimerSession(user.getId(), todo, now, targetSeconds, participationType, groupId, isTaskPublic, isTimerPublic);
            case STOPWATCH -> FocusSession.createStopwatchSession(user.getId(), todo, now, participationType, groupId, isTaskPublic, isTimerPublic);
            case POMODORO -> FocusSession.createPomodoroSession(user.getId(), todo, now, focusDuration, breakDuration, repeatCount, participationType, groupId, isTaskPublic, isTimerPublic);
        };
    }

    private void ensureNoActiveSession(UUID userId) {
        activeFocusSessionRepository.findByUserId(userId)
                .ifPresent(active -> {
                    throw new CustomException(ErrorCode.ACTIVE_SESSION_EXISTS);
                });
    }

    private TimerResponse startCommon(UUID userId, UUID groupId, LocalDateTime now, FocusSession focusSession, Todo todo, PomodoroPhaseType phaseType, Integer round, ParticipationType participationType) {
        FocusSession savedFocusSession = focusSessionRepository.save(focusSession);

        ActiveFocusSession activeSession = ActiveFocusSession.create(
                focusSession.getId(),
                userId,
                now
        );
        activeFocusSessionRepository.save(activeSession);

        // 개인 타이머 활성화 시 isActive 업데이트
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        boolean wasActive = user.getIsActive();
        log.info("타이머 시작: userId={}, wasActive={}, willSetActive=true", userId, wasActive);
        user.setActive(true);
        userRepository.save(user);
        
        // 상태가 변경되었으면 브로드캐스트 (트랜잭션 커밋 후)
        if (!wasActive) {
            log.info("타이머 시작: isActive 상태 변경 감지, 브로드캐스트 예약: userId={}, isActive=true", userId);
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        log.info("타이머 시작: 트랜잭션 커밋 완료, 브로드캐스트 실행: userId={}, isActive=true", userId);
                        userActiveStatusService.broadcastActiveStatusChange(userId, true);
                    }
                }
            );
        } else {
            log.info("타이머 시작: isActive 상태 변경 없음 (이미 true), 브로드캐스트 안 함: userId={}", userId);
        }

        // 그룹 내 개인 타이머인 경우 그룹 참여 상태를 PARTICIPATING으로 변경
        if (participationType == ParticipationType.GROUP && groupId != null) {
            UserGroup userGroup = userGroupRepository.findByUser_IdAndGroup_Id(userId, groupId)
                    .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
            
            if (userGroup.getEnteredAt() != null) {
                // 그룹에 입장한 상태에서 개인 타이머를 시작하면 PARTICIPATING으로 변경
                userGroup.setParticipationStatus(GroupParticipationStatus.PARTICIPATING);
                userGroupRepository.save(userGroup);
                
                // 그룹 멤버 상태 변경 브로드캐스트
                groupMemberStatusService.broadcastMemberStatusUpdate(groupId, userId);
            }
        }

        FocusInterval focusInterval = FocusInterval.create(
                focusSession.getId(),
                now,
                phaseType,
                round
        );
        focusIntervalRepository.save(focusInterval);

        // 시작 시점에 Todo의 actualTimeInSeconds를 기준으로 progressRate 계산 (그룹 타이머 제외)
        Integer progressRate = calculateProgressRateFromTodo(todo);
        savedFocusSession.setProgressRate(progressRate);
        focusSessionRepository.save(savedFocusSession);

        // 타이머 완료 알림 스케줄링 (실패해도 기존 로직에는 영향 없음)
        try {
            timerCompletionNotificationService.scheduleCompletionNotification(savedFocusSession.getId());
        } catch (Exception e) {
            log.warn("타이머 완료 알림 스케줄링 실패 (sessionId: {}): {}", savedFocusSession.getId(), e.getMessage());
        }

        return TimerResponse.fromStart(savedFocusSession, todo);
    }

    private Todo getValidatedTodo(UUID userId, UUID todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));

        if (todo.getIsDeleted()) {
            throw new CustomException(ErrorCode.TODO_DELETED);
        }

        if (!todo.getCategory().getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_TODO_ACCESS);
        }

        return todo;
    }

    private ActiveFocusSession getValidatedActiveFocusSession(UUID userId, UUID sessionId) {
        ActiveFocusSession activeSession = activeFocusSessionRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_SESSION_NOT_FOUND));

        if (!activeSession.getSessionId().equals(sessionId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACTIVE_SESSION);
        }

        return activeSession;
    }

    private FocusSession getValidatedFocusSession(UUID userId, UUID sessionId) {
        FocusSession focusSession = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        if (!focusSession.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_SESSION);
        }

        return focusSession;
    }

    private FocusInterval getLatestInterval(UUID sessionId) {
        return focusIntervalRepository.findTopBySessionIdOrderByStartedAtDesc(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERVAL_NOT_FOUND));
    }


    private LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    private long calculateIntervalDurationSeconds(FocusInterval focusInterval) {
        return Duration.between(focusInterval.getStartedAt(), focusInterval.getEndedAt()).getSeconds();
    }

    private void validateResumableState(FocusSession session) {
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

    private void validatePauseableState(FocusSession session) {
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

    private void validateFinishableState(FocusSession session) {
        TimerStatus status = session.getStatus();

        if (status == TimerStatus.FINISHED) {
            throw new CustomException(ErrorCode.SESSION_ALREADY_FINISHED);
        }

        if (status != TimerStatus.RUNNING && status != TimerStatus.PAUSED) {
            throw new CustomException(ErrorCode.SESSION_NOT_FINISHABLE);
        }
    }

    //========= 뽀모도로 관련 메서드 ============

    private long calculateAccumulatedPhaseSeconds(List<FocusInterval> intervals, PomodoroPhaseType phaseType, Integer round, LocalDateTime now) {
        return intervals.stream()
                .filter(i -> i.getPhaseType() == phaseType)
                .filter(i -> i.getRound().equals(round))
                .mapToLong(i -> {
                    LocalDateTime end = (i.getEndedAt() != null) ? i.getEndedAt() : now;
                    return Duration.between(i.getStartedAt(), end).getSeconds();
                })
                .sum();
    }

    private boolean isPhaseFinished(FocusSession session, PomodoroPhaseType phase, long accumulatedSeconds) {
        if (phase == PomodoroPhaseType.FOCUS) {
            return accumulatedSeconds >= session.getFocusDuration();
        } else {
            return accumulatedSeconds >= session.getBreakDuration();
        }
    }

    private int calculateCompletedFocusRounds(List<FocusInterval> intervals) {
        return (int) intervals.stream()
                .filter(i -> i.getPhaseType() == PomodoroPhaseType.FOCUS)
                .map(FocusInterval::getRound)
                .distinct()
                .count();
    }

    private boolean isPomodoroFinished(FocusSession session, List<FocusInterval> intervals) {
        int completedRounds = calculateCompletedFocusRounds(intervals);
        return completedRounds >= session.getRepeatCount();
    }

    private PomodoroPhaseType nextPhase(PomodoroPhaseType current) {
        return current == PomodoroPhaseType.FOCUS
                ? PomodoroPhaseType.BREAK
                : PomodoroPhaseType.FOCUS;
    }

    private FocusInterval startPhaseInterval(FocusSession session, PomodoroPhaseType phase, LocalDateTime now, Integer round) {
        FocusInterval interval = FocusInterval.create(
                session.getId(),
                now,
                phase,
                round
        );
        return focusIntervalRepository.save(interval);
    }
}
