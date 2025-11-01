package com.mogakjak.mogakjak.domain.timer.service;

import com.mogakjak.mogakjak.domain.timer.dto.request.TimerStartRequest;
import com.mogakjak.mogakjak.domain.timer.entity.TimerInterval;
import com.mogakjak.mogakjak.domain.timer.entity.TimerSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.TimerIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.TimerSessionRepository;
import com.mogakjak.mogakjak.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

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

        // 첫 interval 생성 (시작 시간 기록)
        TimerInterval interval = TimerInterval.builder()
                .sessionId(saved.getId())
                .startedAt(now)
                .build();

        intervalRepository.save(interval);

        return saved;
    }

    @Override
    @Transactional
    public void pauseTimer(User user) {
        TimerSession session = sessionRepository.findByUserIdAndStatus(user.getId(), TimerStatus.RUNNING)
                .orElseThrow(() -> new IllegalStateException("진행 중인 타이머가 없습니다."));

        LocalDateTime now = LocalDateTime.now();

        // 마지막 interval 종료
        List<TimerInterval> intervals = intervalRepository.findAllBySessionId(session.getId());
        TimerInterval last = intervals.get(intervals.size() - 1);
        last = TimerInterval.builder()
                .id(last.getId())
                .sessionId(last.getSessionId())
                .startedAt(last.getStartedAt())
                .endedAt(now)
                .build();
        intervalRepository.save(last);

        // 상태 변경
        session = TimerSession.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .mode(session.getMode())
                .startedAt(session.getStartedAt())
                .targetDuration(session.getTargetDuration())
                .status(TimerStatus.PAUSED)
                .build();
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void resumeTimer(User user) {
        TimerSession session = sessionRepository.findByUserIdAndStatus(user.getId(), TimerStatus.PAUSED)
                .orElseThrow(() -> new IllegalStateException("일시정지된 타이머가 없습니다."));

        LocalDateTime now = LocalDateTime.now();

        // 새 interval 생성
        TimerInterval newInterval = TimerInterval.builder()
                .sessionId(session.getId())
                .startedAt(now)
                .build();
        intervalRepository.save(newInterval);

        // 상태 복귀
        session = TimerSession.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .mode(session.getMode())
                .startedAt(session.getStartedAt())
                .targetDuration(session.getTargetDuration())
                .status(TimerStatus.RUNNING)
                .build();
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public TimerSession stopTimer(User user) {
        TimerSession session = sessionRepository.findByUserIdAndStatus(user.getId(), TimerStatus.RUNNING)
                .orElseThrow(() -> new IllegalStateException("진행 중인 타이머가 없습니다."));

        LocalDateTime now = LocalDateTime.now();

        // 마지막 interval 종료
        List<TimerInterval> intervals = intervalRepository.findAllBySessionId(session.getId());
        TimerInterval last = intervals.get(intervals.size() - 1);
        last = TimerInterval.builder()
                .id(last.getId())
                .sessionId(last.getSessionId())
                .startedAt(last.getStartedAt())
                .endedAt(now)
                .build();
        intervalRepository.save(last);

        // 총 집중시간 계산
        long totalSeconds = intervals.stream()
                .filter(i -> i.getStartedAt() != null && i.getEndedAt() != null)
                .mapToLong(i -> Duration.between(i.getStartedAt(), i.getEndedAt()).getSeconds())
                .sum();

        session = TimerSession.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .mode(session.getMode())
                .startedAt(session.getStartedAt())
                .endedAt(now)
                .targetDuration(session.getTargetDuration())
                .totalDuration(totalSeconds)
                .status(TimerStatus.FINISHED)
                .build();

        return sessionRepository.save(session);
    }

    /**
     * 기존 세션 종료 처리 (중복 세션 방지)
     */
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
