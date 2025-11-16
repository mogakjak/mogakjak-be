package com.mogakjak.mogakjak.domain.timer.entity;

import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FocusSession extends BaseSchema {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID todoId;

    @Enumerated(EnumType.STRING)
    private TimerMode mode;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Long targetDuration;

    private Long totalDuration;

    private Integer progressRate;

    // 뽀모도로 용
    private Long focusDuration;
    private Long breakDuration;
    private Integer repeatCount;

    @Enumerated(EnumType.STRING)
    private TimerStatus status;

    public static FocusSession createTimerSession(UUID userId, UUID todoId, LocalDateTime startedAt, Long targetDuration) {
        return new FocusSession(
                userId,
                todoId,
                TimerMode.TIMER,
                startedAt,
                null,
                targetDuration,
                0L,
                0,
                null,
                null,
                null,
                TimerStatus.RUNNING
        );
    }

    public static FocusSession createStopWatchSession(UUID userId, UUID todoId, LocalDateTime startedAt) {
        return new FocusSession(
                userId,
                todoId,
                TimerMode.STOPWATCH,
                startedAt,
                null,
                null,
                0L,
                0,
                null,
                null,
                null,
                TimerStatus.RUNNING
        );
    }

    // TODO: progressRate 유효성 검사 고민
    public void pause(Integer progressRate) {
        this.status = TimerStatus.PAUSED;
        this.progressRate = progressRate;
    }

    public void resume() {
        this.status = TimerStatus.RUNNING;
    }

    public void end(LocalDateTime endedAt, Integer progressRate) {
        this.status = TimerStatus.FINISHED;
        this.endedAt = endedAt;
        this.progressRate = progressRate;
    }

    public void addDuration(Long seconds) {
        if (this.totalDuration == null) {
            this.totalDuration = 0L;
        }
        this.totalDuration += seconds;
    }
}
