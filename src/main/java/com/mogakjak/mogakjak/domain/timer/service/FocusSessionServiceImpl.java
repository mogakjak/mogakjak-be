package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.request.StopwatchStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerResponse;
import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
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

        FocusSession focusSession = createFocusSession(TimerMode.TIMER, user, request.todoId(), now, request.targetSeconds());

        return startCommon(user.getId(), request.todoId(), now, focusSession, todo);
    }

    @Override
    @Transactional
    public TimerResponse startStopWatch(User user, StopwatchStartRequest request) {
        LocalDateTime now = getCurrentTime();

        ensureNoActiveSession(user.getId());
        Todo todo = getValidatedTodo(user.getId(), request.todoId());

        FocusSession focusSession = createFocusSession(TimerMode.STOPWATCH, user, request.todoId(), now, null);

        return startCommon(user.getId(), request.todoId(), now, focusSession, todo);
    }

    @Override
    @Transactional
    public TimerResponse pauseTimer(User user, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        getValidatedActiveFocusSession(user.getId(), sessionId);
        FocusSession currentFocusSession = getValidatedFocusSession(user.getId(), sessionId);
        FocusInterval currentInterval = getLatestInterval(sessionId);

        validatePauseableState(currentFocusSession);

        currentInterval.end(now);
        long intervalDurationSeconds = calculateIntervalDurationSeconds(currentInterval);

        currentFocusSession.pause(intervalDurationSeconds);

        return TimerResponse.fromPause(currentFocusSession, now);
    }

    @Override
    @Transactional
    public TimerResponse resumeTimer(User user, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        getValidatedActiveFocusSession(user.getId(), sessionId);
        FocusSession currentFocusSession = getValidatedFocusSession(user.getId(), sessionId);

        validateResumableState(currentFocusSession);

        FocusInterval focusInterval = FocusInterval.create(
                currentFocusSession.getId(),
                now
        );
        focusIntervalRepository.save(focusInterval);

        currentFocusSession.resume();

        return TimerResponse.fromResume(currentFocusSession);
    }

    @Override
    @Transactional
    public TimerResponse finishTimer(User user, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        ActiveFocusSession currentActiveSession = getValidatedActiveFocusSession(user.getId(), sessionId);
        FocusSession currentFocusSession = getValidatedFocusSession(user.getId(), sessionId);
        FocusInterval currentInterval = getLatestInterval(sessionId);

        validateFinishableState(currentFocusSession);

        long intervalDurationSeconds = 0L;
        if (currentFocusSession.getStatus() != TimerStatus.PAUSED) {
            currentInterval.end(now);
            intervalDurationSeconds = calculateIntervalDurationSeconds(currentInterval);
        }

        activeFocusSessionRepository.deleteById(currentActiveSession.getId());

        currentFocusSession.end(now, intervalDurationSeconds);

        return TimerResponse.fromFinish(currentFocusSession);
    }

    private FocusSession createFocusSession(TimerMode mode, User user, UUID todoId, LocalDateTime now, Long targetSeconds) {
        return switch (mode) {
            case TIMER -> FocusSession.createTimerSession(user.getId(), todoId, now, targetSeconds);
            case STOPWATCH -> FocusSession.createStopWatchSession(user.getId(), todoId, now);
            case POMODORO -> FocusSession.createStopWatchSession(user.getId(), todoId, now); // 아직 포모도로 구현 전이라 가안으로!
        };
    }

    private void ensureNoActiveSession(UUID userId) {
        activeFocusSessionRepository.findByUserId(userId)
                .ifPresent(active -> {
                    throw new CustomException(ErrorCode.ACTIVE_SESSION_EXISTS);
                });
    }

    private TimerResponse startCommon(UUID userId, UUID sessionId, LocalDateTime now, FocusSession focusSession, Todo todo) {
        FocusSession savedFocusSession = focusSessionRepository.save(focusSession);

        ActiveFocusSession activeSession = ActiveFocusSession.create(
                sessionId,
                userId,
                now
        );
        activeFocusSessionRepository.save(activeSession);

        FocusInterval focusInterval = FocusInterval.create(
                sessionId,
                now
        );
        focusIntervalRepository.save(focusInterval);

        return TimerResponse.fromStart(savedFocusSession, todo);
    }

    private Todo getValidatedTodo(UUID userId, UUID todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));

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

//
//    @Override
//    @Transactional
//    public void nextPomodoroPhase(User user, UUID sessionId) {
//        TimerSession session = sessionRepository.findById(sessionId)
//                .orElseThrow(() -> new CustomException(ErrorCode.TIMER_NOT_FOUND));
//
//        if (!session.getUserId().equals(user.getId())) {
//            throw new CustomException(ErrorCode.FORBIDDEN_TIMER_ACCESS);
//        }
//        if (session.getMode() != TimerMode.POMODORO) {
//            throw new CustomException(ErrorCode.INVALID_POMODORO_SESSION);
//        }
//
//        List<TimerInterval> intervals = intervalRepository.findAllBySessionId(session.getId());
//        if (intervals.isEmpty()) {
//            createPomodoroFocus(session, LocalDateTime.now(), 1);
//            return;
//        }
//
//        TimerInterval last = intervals.getLast();
//
//        // 지금까지 완료된 집중 구간 개수
//        long doneFocusCount = intervals.stream()
//                .filter(it -> it.getType() == IntervalType.FOCUS)
//                .count();
//
//        // 반복 다 끝났으면 종료
//        if (session.getRepeatCount() != null && doneFocusCount >= session.getRepeatCount()) {
//            finishPomodoro(session);
//            return;
//        }
//
//        LocalDateTime now = LocalDateTime.now();
//
//        if (last.getType() == IntervalType.FOCUS) {
//            // 집중 끝났으니 휴식으로
//            createPomodoroBreak(session, now, (int) doneFocusCount);
//        } else {
//            // 휴식 끝났으니 다음 집중으로
//            createPomodoroFocus(session, now, (int) doneFocusCount + 1);
//        }
//    }
//
//    @Override
//    public List<DailyFocusStatsResponse> getDailyFocusDurations(User user) {
//        List<Object[]> rows = intervalRepository.findDailyFocusDurationsByUser(user.getId());
//
//        return rows.stream()
//                .map(row -> new DailyFocusStatsResponse(
//                        ((java.sql.Date) row[0]).toLocalDate(),
//                        ((Number) row[1]).longValue()
//                ))
//                .toList();
//    }
}
