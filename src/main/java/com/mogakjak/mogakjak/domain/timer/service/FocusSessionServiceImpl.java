package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerResponse;
import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
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

        activeFocusSessionRepository.findByUserId(user.getId())
                .ifPresent(active -> {
                    throw new CustomException(ErrorCode.ACTIVE_SESSION_EXISTS);
                });

        todoRepository.findById(request.todoId())
                .orElseThrow(() -> new CustomException(ErrorCode.TODO_NOT_FOUND));

        // 통합 관리용 세션 생성 및 저장
        FocusSession focusSession = FocusSession.createTimerSession(
                user.getId(),
                request.todoId(),
                now,
                request.targetSeconds()
        );
        FocusSession savedFocusSession = focusSessionRepository.save(focusSession);

        // 활성 세션 생성
        ActiveFocusSession activeSession = ActiveFocusSession.create(
                savedFocusSession.getId(),
                user.getId(),
                now
        );
        activeFocusSessionRepository.save(activeSession);

        // 인터벌 생성
        FocusInterval focusInterval = FocusInterval.create(
                savedFocusSession.getId(),
                now
        );
        focusIntervalRepository.save(focusInterval);

        return TimerResponse.fromStartAndResume(savedFocusSession);
    }

    @Override
    @Transactional
    public TimerResponse pauseTimer(User user, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        // TODO: 유효성 검사 메서드 뽑아서 한 곳에서 관리
        // 활성 세션 있는지 확인
        ActiveFocusSession currentActiveSession = activeFocusSessionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_SESSION_NOT_FOUND));
        if (!currentActiveSession.getSessionId().equals(sessionId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACTIVE_SESSION);
        }

        // 집중 세션 있는지 확인
        FocusSession currentFocusSession = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        // 인터벌 존재하는지 확인
        FocusInterval currentInterval = focusIntervalRepository.findTopBySessionIdOrderByStartedAtDesc(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERVAL_NOT_FOUND));

        // 정지 가능한 상태에 있는지 확인
        validatePauseableState(currentFocusSession);

        // 인터벌 종료
        currentInterval.end(now);
        long intervalDurationSeconds = calculateIntervalDurationSeconds(currentInterval);

        // 활성 세션은 그대로 유지

        // 집중 세션 상태 PAUSED로 변경 + 누적 몰입 시간 추가
        currentFocusSession.pause(intervalDurationSeconds);

        return TimerResponse.fromPause(currentFocusSession, now);
    }

    @Override
    @Transactional
    public TimerResponse resumeTimer(User user, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        // 활성 세션 있는지 확인
        ActiveFocusSession currentActiveSession = activeFocusSessionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_SESSION_NOT_FOUND));
        if (!currentActiveSession.getSessionId().equals(sessionId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACTIVE_SESSION);
        }

        // 집중 세션 있는지 확인
        FocusSession currentFocusSession = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        validateResumableState(currentFocusSession);

        // 인터벌 생성
        FocusInterval focusInterval = FocusInterval.create(
                currentFocusSession.getId(),
                now
        );
        focusIntervalRepository.save(focusInterval);

        // 집중 세션 상태 변경
        currentFocusSession.resume();

        // 활성 세션은 그대로 유지

        return TimerResponse.fromStartAndResume(currentFocusSession);
    }

    @Override
    @Transactional
    public TimerResponse finishTimer(User user, UUID sessionId) {
        LocalDateTime now = getCurrentTime();

        // 활성 세션 있는지 확인
        ActiveFocusSession currentActiveSession = activeFocusSessionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVE_SESSION_NOT_FOUND));
        if (!currentActiveSession.getSessionId().equals(sessionId)) {
            throw new CustomException(ErrorCode.FORBIDDEN_ACTIVE_SESSION);
        }

        // 집중 세션 있는지 확인
        FocusSession currentFocusSession = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        // 인터벌 존재하는지 확인
        FocusInterval currentInterval = focusIntervalRepository.findTopBySessionIdOrderByStartedAtDesc(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERVAL_NOT_FOUND));

        // 중지 가능한 상태에 있는지 확인
        validateFinishableState(currentFocusSession);

        // 인터벌 종료
        if (currentFocusSession.getStatus() != TimerStatus.PAUSED) currentInterval.end(now);
        long intervalDurationSeconds = calculateIntervalDurationSeconds(currentInterval);

        // 활성 세션 종료 (세션 삭제)
        activeFocusSessionRepository.deleteById(currentActiveSession.getId());

        // 집중 세션 상태 변경 및 종료 시간 기입
        currentFocusSession.end(now, intervalDurationSeconds);

        return TimerResponse.fromFinish(currentFocusSession);
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
