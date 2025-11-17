package com.mogakjak.mogakjak.domain.timer.service;

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
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusSessionServiceImpl implements FocusSessionService {

    private final FocusSessionRepository focusSessionRepository;
    private final FocusIntervalRepository focusIntervalRepository;
    private final ActiveFocusSessionRepository activeFocusSessionRepository;
    private final TodoRepository todoRepository;

    @Override
    @Transactional
    public TimerResponse startTimer(User user, TimerStartRequest request) {
        LocalDateTime now = getCurrentTime();

        ensureNoActiveSession(user.getId());
        Todo todo = getValidatedTodo(user.getId(), request.todoId());

        FocusSession focusSession = createFocusSession(TimerMode.TIMER, user, todo, now, request.targetSeconds(), null, null, null);

        return startCommon(user.getId(), now, focusSession, todo, PomodoroPhaseType.NORMAL, 0);
    }

    @Override
    @Transactional
    public TimerResponse startStopwatch(User user, StopwatchStartRequest request) {
        LocalDateTime now = getCurrentTime();

        ensureNoActiveSession(user.getId());
        Todo todo = getValidatedTodo(user.getId(), request.todoId());

        FocusSession focusSession = createFocusSession(TimerMode.STOPWATCH, user, todo, now, null, null, null, null);

        return startCommon(user.getId(), now, focusSession, todo, PomodoroPhaseType.NORMAL, 0);
    }

    @Override
    @Transactional
    public TimerResponse startPomodoro(User user, PomodoroStartRequest request) {
        LocalDateTime now = getCurrentTime();

        ensureNoActiveSession(user.getId());
        Todo todo = getValidatedTodo(user.getId(), request.todoId());

        FocusSession focusSession = createFocusSession(TimerMode.POMODORO, user, todo, now, null, request.focusSeconds(), request.breakSeconds(), request.repeatCount());

        return startCommon(user.getId(), now, focusSession, todo, PomodoroPhaseType.FOCUS, 1);
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
        Integer progressRate = calculateProgressRate(todo.getTargetTimeInSeconds(), currentFocusSession.getTotalDuration());
        currentFocusSession.pause(progressRate);

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

        currentFocusSession.addDuration(intervalDurationSeconds);
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
            focusSession.end(now, 100);
            activeFocusSessionRepository.deleteById(currentActiveSession.getId());
            return TimerResponse.fromFinish(focusSession);
        }

        PomodoroPhaseType nextPhase = nextPhase(currentPhase);
        int nextRound = nextPhase == PomodoroPhaseType.FOCUS ? currentRound + 1 : currentRound;

        FocusInterval nextPhaseInterval = startPhaseInterval(focusSession, nextPhase, now, nextRound);

        return TimerResponse.fromPomodoroPhaseChange(focusSession, nextPhaseInterval);
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

    private FocusSession createFocusSession(TimerMode mode, User user, Todo todo, LocalDateTime now, Long targetSeconds, Long focusDuration, Long breakDuration, Integer repeatCount) {
        return switch (mode) {
            case TIMER -> FocusSession.createTimerSession(user.getId(), todo, now, targetSeconds);
            case STOPWATCH -> FocusSession.createStopwatchSession(user.getId(), todo, now);
            case POMODORO -> FocusSession.createPomodoroSession(user.getId(), todo, now, focusDuration, breakDuration, repeatCount);
        };
    }

    private void ensureNoActiveSession(UUID userId) {
        activeFocusSessionRepository.findByUserId(userId)
                .ifPresent(active -> {
                    throw new CustomException(ErrorCode.ACTIVE_SESSION_EXISTS);
                });
    }

    private TimerResponse startCommon(UUID userId, LocalDateTime now, FocusSession focusSession, Todo todo, PomodoroPhaseType phaseType, Integer round) {
        FocusSession savedFocusSession = focusSessionRepository.save(focusSession);

        ActiveFocusSession activeSession = ActiveFocusSession.create(
                focusSession.getId(),
                userId,
                now
        );
        activeFocusSessionRepository.save(activeSession);

        FocusInterval focusInterval = FocusInterval.create(
                focusSession.getId(),
                now,
                phaseType,
                round
        );
        focusIntervalRepository.save(focusInterval);

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
