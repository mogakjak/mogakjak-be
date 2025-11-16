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

    private Integer calculateProgressRate(Long targetDuration, Long totalDuration) {
        if (targetDuration == null || targetDuration <= 0) {
            return null;
        }
        if (totalDuration == null || totalDuration <= 0) {
            return 0;
        }

        double rate = (double) totalDuration / targetDuration * 100;
        return (int) Math.min(100, Math.floor(rate));
    }

    private void addDuration(Long seconds) {
        if (this.totalDuration == null) {
            this.totalDuration = 0L;
        }
        this.totalDuration += seconds;
    }

    // TODO: progressRate 유효성 검사 고민
    public void pause(long intervalDurationSeconds) {
        this.status = TimerStatus.PAUSED;
        addDuration(intervalDurationSeconds);
        this.progressRate = calculateProgressRate(this.targetDuration, this.totalDuration);
    }

    public void resume() {
        this.status = TimerStatus.RUNNING;
    }

    public void end(LocalDateTime endedAt, long intervalDurationSeconds) {
        this.status = TimerStatus.FINISHED;
        this.endedAt = endedAt;
        addDuration(intervalDurationSeconds);
        this.progressRate = calculateProgressRate(this.targetDuration, this.totalDuration);
    }
}
