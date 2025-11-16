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
        long intervalDurationSeconds = Duration.between(currentInterval.getStartedAt(), currentInterval.getEndedAt()).getSeconds();

        // 활성 세션은 그대로 유지

        // 집중 세션 상태 PAUSED로 변경 + 누적 몰입 시간 추가
        currentFocusSession.addDuration(intervalDurationSeconds);

        Integer progressRate = calculateProgressRate(currentFocusSession.getTargetDuration(), currentFocusSession.getTotalDuration());
        currentFocusSession.pause(progressRate);

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

    private LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    private static Integer calculateProgressRate(Long totalDuration, Long targetDuration) {
        if (targetDuration == null || targetDuration <= 0) {
            return null;
        }
        if (totalDuration == null || totalDuration <= 0) {
            return 0;
        }

        double rate = (double) totalDuration / targetDuration * 100;
        return (int) Math.min(100, Math.floor(rate));
    }

    private void validatePauseableState(FocusSession currentFocusSession) {
        if (currentFocusSession.getStatus() != TimerStatus.RUNNING) {
            if (currentFocusSession.getStatus() == TimerStatus.PAUSED) {
                throw new CustomException(ErrorCode.SESSION_ALREADY_PAUSED);
            }
            if (currentFocusSession.getStatus() == TimerStatus.FINISHED) {
                throw new CustomException(ErrorCode.SESSION_ALREADY_FINISHED);
            }

            throw new CustomException(ErrorCode.SESSION_NOT_RUNNING);
        }
    }


//
//    @Override
//    @Transactional
//    public void resumeTimer(User user) {
//        TimerSession session = sessionRepository.findByUserIdAndStatus(user.getId(), TimerStatus.PAUSED)
//                .orElseThrow(() -> new CustomException(ErrorCode.TIMER_NOT_PAUSED));
//
//        LocalDateTime now = LocalDateTime.now();
//
//        if (session.getMode() == TimerMode.POMODORO) {
//            List<TimerInterval> intervals = intervalRepository.findAllBySessionId(session.getId());
//            if (intervals.isEmpty()) {
//                createPomodoroFocus(session, now, 1); // 이렇게 복구 로직이 있는 것 / 상응하는 에러를 던지는 것에 대한 추가 고민 필요
//            } else {
//                TimerInterval last = intervals.getLast();
//                intervalRepository.save(
//                        TimerInterval.builder()
//                                .sessionId(session.getId())
//                                .startedAt(now)
//                                .type(last.getType())
//                                .round(last.getRound())
//                                .build()
//                );
//            }
//        } else {
//            createNormalInterval(session, now);
//        }
//
//        TimerSession running = session.toBuilder()
//                .status(RUNNING)
//                .build();
//        sessionRepository.save(running);
//    }
//
//    @Override
//    @Transactional
//    public TimerStopResponse stopTimer(User user) {
//        TimerSession session = sessionRepository.findByUserIdAndStatus(user.getId(), RUNNING)
//                .or(() -> sessionRepository.findByUserIdAndStatus(user.getId(), TimerStatus.PAUSED))
//                .orElseThrow(() -> new CustomException(ErrorCode.NO_ACTIVE_TIMER_SESSION));
//
//        LocalDateTime now = LocalDateTime.now();
//
//        List<TimerInterval> intervals = intervalRepository.findAllBySessionId(session.getId());
//        if (!intervals.isEmpty()) {
//            TimerInterval last = intervals.getLast();
//            if (last.getEndedAt() == null) {
//                TimerInterval closed = TimerInterval.builder()
//                        .id(last.getId())
//                        .sessionId(last.getSessionId())
//                        .startedAt(last.getStartedAt())
//                        .endedAt(now)
//                        .type(last.getType())
//                        .round(last.getRound())
//                        .build();
//                intervalRepository.save(closed);
//            }
//        }
//
//        long totalSeconds = intervalRepository.findAllBySessionId(session.getId()).stream()
//                .filter(i -> i.getStartedAt() != null && i.getEndedAt() != null)
//                .mapToLong(i -> Duration.between(i.getStartedAt(), i.getEndedAt()).getSeconds())
//                .sum();
//
//        TimerSession finished = session.toBuilder()
//                .endedAt(now)
//                .totalDuration(totalSeconds)
//                .status(TimerStatus.FINISHED)
//                .build();
//
//        TimerSession saved = sessionRepository.save(finished);
//
//        Long safeTarget = saved.getTargetDuration() == null ? 0L : saved.getTargetDuration();
//
//        return TimerStopResponse.builder()
//                .sessionId(saved.getId())
//                .mode(saved.getMode())
//                .status(saved.getStatus())
//                .startedAt(saved.getStartedAt())
//                .endedAt(saved.getEndedAt())
//                .targetDuration(safeTarget)
//                .totalDuration(saved.getTotalDuration())
//                .build();
//    }
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
//
//    private void finishPomodoro(TimerSession session) {
//        sessionRepository.save(
//                session.toBuilder()
//                        .endedAt(LocalDateTime.now())
//                        .status(TimerStatus.FINISHED)
//                        .build()
//        );
//    }
//
//    private void createPomodoroFocus(TimerSession session, LocalDateTime now, int round) {
//        intervalRepository.save(
//                TimerInterval.builder()
//                        .sessionId(session.getId())
//                        .startedAt(now)
//                        .type(IntervalType.FOCUS)
//                        .round(round)
//                        .build()
//        );
//    }
//
//    private void createPomodoroBreak(TimerSession session, LocalDateTime now, int round) {
//        intervalRepository.save(
//                TimerInterval.builder()
//                        .sessionId(session.getId())
//                        .startedAt(now)
//                        .type(IntervalType.BREAK)
//                        .round(round)
//                        .build()
//        );
//    }
//
//    private TimerInterval createNormalInterval(TimerSession session, LocalDateTime now) {
//        return intervalRepository.save(
//                TimerInterval.builder()
//                        .sessionId(session.getId())
//                        .startedAt(now)
//                        .type(IntervalType.NORMAL)
//                        .build()
//        );
//    }
//
//    private void pauseOrFinishSession(TimerSession session, TimerStatus newStatus) {
//        sessionRepository.save(
//                session.toBuilder()
//                        .endedAt(LocalDateTime.now())
//                        .status(newStatus)
//                        .build()
//        );
//    }
}
