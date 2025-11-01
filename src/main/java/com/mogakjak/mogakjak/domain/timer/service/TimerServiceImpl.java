package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.dto.response.TimerStopResponse;
import com.mogakjak.mogakjak.domain.timer.entity.TimerInterval;
import com.mogakjak.mogakjak.domain.timer.entity.TimerSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.TimerIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.TimerSessionRepository;
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
public class TimerServiceImpl implements TimerService {

    private final TimerSessionRepository sessionRepository;
    private final TimerIntervalRepository intervalRepository;

    @Override
    @Transactional
    public TimerSession startTimer(User user, TimerStartRequest request) {
        // 이미 진행 중인 세션이 있으면 종료 처리
        sessionRepository.findByUserIdAndStatus(user.getId(), TimerStatus.RUNNING)
                .ifPresent(session -> pauseOrFinishSession(session, TimerStatus.FINISHED));

        LocalDateTime now = LocalDateTime.now();

        // 세션 생성
        TimerSession session = TimerSession.builder()
                .userId(user.getId())
                .mode(request.timerMode())
                .startedAt(now)
                .targetDuration(request.targetSeconds())
                .focusDuration(request.focusSeconds())
                .breakDuration(request.breakSeconds())
                .repeatCount(request.repeatCount())
                .status(TimerStatus.RUNNING)
                .build();

        TimerSession saved = sessionRepository.save(session);

        if (request.timerMode() == TimerMode.POMODORO) {
            createPomodoroFocus(saved, now, 1);
        } else {
            createNormalInterval(saved, now);
        }

        return saved;
    }

    @Override
    @Transactional
    public void pauseTimer(User user) {
        TimerSession session = sessionRepository.findByUserIdAndStatus(user.getId(), TimerStatus.RUNNING)
                .orElseThrow(() -> new CustomException(ErrorCode.TIMER_NOT_RUNNING));

        LocalDateTime now = LocalDateTime.now();

        // 마지막 interval 종료
        List<TimerInterval> intervals = intervalRepository.findAllBySessionId(session.getId());
        TimerInterval last = intervals.getLast();

        TimerInterval closed = TimerInterval.builder()
                .id(last.getId())
                .sessionId(last.getSessionId())
                .startedAt(last.getStartedAt())
                .endedAt(now)
                .build();
        intervalRepository.save(closed);

        // 상태 변경
        TimerSession paused = TimerSession.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .mode(session.getMode())
                .startedAt(session.getStartedAt())
                .targetDuration(session.getTargetDuration())
                .focusDuration(session.getFocusDuration())
                .breakDuration(session.getBreakDuration())
                .repeatCount(session.getRepeatCount())
                .status(TimerStatus.PAUSED)
                .build();
        sessionRepository.save(paused);
    }

    @Override
    @Transactional
    public void resumeTimer(User user) {
        TimerSession session = sessionRepository.findByUserIdAndStatus(user.getId(), TimerStatus.PAUSED)
                .orElseThrow(() -> new CustomException(ErrorCode.TIMER_NOT_PAUSED));

        LocalDateTime now = LocalDateTime.now();

        if (session.getMode() == TimerMode.POMODORO) {
            List<TimerInterval> intervals = intervalRepository.findAllBySessionId(session.getId());
            TimerInterval last = intervals.getLast();

            intervalRepository.save(
                    TimerInterval.builder()
                            .sessionId(session.getId())
                            .startedAt(now)
                            .type(last.getType())
                            .round(last.getRound())
                            .build()
            );
        } else {
            createNormalInterval(session, now);
        }

        TimerSession running = TimerSession.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .mode(session.getMode())
                .startedAt(session.getStartedAt())
                .targetDuration(session.getTargetDuration())
                .focusDuration(session.getFocusDuration())
                .breakDuration(session.getBreakDuration())
                .repeatCount(session.getRepeatCount())
                .status(TimerStatus.RUNNING)
                .build();
        sessionRepository.save(running);
    }

    @Override
    @Transactional
    public TimerStopResponse stopTimer(User user) {
        TimerSession session = sessionRepository.findByUserIdAndStatus(user.getId(), TimerStatus.RUNNING)
                .orElseThrow(() -> new CustomException(ErrorCode.TIMER_NOT_RUNNING));

        LocalDateTime now = LocalDateTime.now();

        List<TimerInterval> intervals = intervalRepository.findAllBySessionId(session.getId());
        TimerInterval last = intervals.get(intervals.size() - 1);

        TimerInterval closed = TimerInterval.builder()
                .id(last.getId())
                .sessionId(last.getSessionId())
                .startedAt(last.getStartedAt())
                .endedAt(now)
                .type(last.getType())
                .round(last.getRound())
                .build();
        intervalRepository.save(closed);

        long totalSeconds = intervalRepository.findAllBySessionId(session.getId()).stream()
                .filter(i -> i.getStartedAt() != null && i.getEndedAt() != null)
                .mapToLong(i -> Duration.between(i.getStartedAt(), i.getEndedAt()).getSeconds())
                .sum();

        TimerSession finished = session.toBuilder()
                .endedAt(now)
                .totalDuration(totalSeconds)
                .status(TimerStatus.FINISHED)
                .build();

        TimerSession saved = sessionRepository.save(finished);

        return TimerStopResponse.builder()
                .sessionId(saved.getId())
                .mode(saved.getMode())
                .status(saved.getStatus())
                .startedAt(saved.getStartedAt())
                .endedAt(saved.getEndedAt())
                .targetDuration(saved.getTargetDuration())
                .totalDuration(saved.getTotalDuration())
                .build();
    }

    @Override
    @Transactional
    public void nextPomodoroPhase(UUID sessionId) {
        TimerSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.TIMER_NOT_FOUND));

        if (session.getMode() != TimerMode.POMODORO) {
            throw new CustomException(ErrorCode.INVALID_POMODORO_SESSION);
        }

        List<TimerInterval> intervals = intervalRepository.findAllBySessionId(session.getId());
        if (intervals.isEmpty()) {
            createPomodoroFocus(session, LocalDateTime.now(), 1);
            return;
        }

        TimerInterval last = intervals.get(intervals.size() - 1);

        // 지금까지 완료된 집중 구간 개수
        long doneFocusCount = intervals.stream()
                .filter(it -> "FOCUS".equals(it.getType()))
                .count();

        // 반복 다 끝났으면 종료
        if (session.getRepeatCount() != null && doneFocusCount >= session.getRepeatCount()) {
            finishPomodoro(session);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        if ("FOCUS".equals(last.getType())) {
            // 집중 끝났으니 휴식으로
            createPomodoroBreak(session, now, (int) doneFocusCount);
        } else {
            // 휴식 끝났으니 다음 집중으로
            createPomodoroFocus(session, now, (int) doneFocusCount + 1);
        }
    }

    private void finishPomodoro(TimerSession session) {
        sessionRepository.save(
                TimerSession.builder()
                        .id(session.getId())
                        .userId(session.getUserId())
                        .mode(session.getMode())
                        .startedAt(session.getStartedAt())
                        .endedAt(LocalDateTime.now())
                        .targetDuration(session.getTargetDuration())
                        .focusDuration(session.getFocusDuration())
                        .breakDuration(session.getBreakDuration())
                        .repeatCount(session.getRepeatCount())
                        .status(TimerStatus.FINISHED)
                        .build()
        );
    }

    private void createPomodoroFocus(TimerSession session, LocalDateTime now, int round) {
        intervalRepository.save(
                TimerInterval.builder()
                        .sessionId(session.getId())
                        .startedAt(now)
                        .type("FOCUS")
                        .round(round)
                        .build()
        );
    }

    private void createPomodoroBreak(TimerSession session, LocalDateTime now, int round) {
        intervalRepository.save(
                TimerInterval.builder()
                        .sessionId(session.getId())
                        .startedAt(now)
                        .type("BREAK")
                        .round(round)
                        .build()
        );
    }

    private void createNormalInterval(TimerSession session, LocalDateTime now) {
        intervalRepository.save(
                TimerInterval.builder()
                        .sessionId(session.getId())
                        .startedAt(now)
                        .type("NORMAL")
                        .build()
        );
    }

    private void pauseOrFinishSession(TimerSession session, TimerStatus newStatus) {
        sessionRepository.save(
                TimerSession.builder()
                        .id(session.getId())
                        .userId(session.getUserId())
                        .mode(session.getMode())
                        .startedAt(session.getStartedAt())
                        .endedAt(LocalDateTime.now())
                        .status(newStatus)
                        .build()
        );
    }
}
