package com.mogakjak.mogakjak.domain.timer.entity;

import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(indexes = {
        @Index(name = "idx_focus_interval_session_started", columnList = "session_id,started_at"),
        @Index(name = "idx_focus_interval_session_phase_end", columnList = "session_id,phase_type,ended_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FocusInterval extends BaseSchema {

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PomodoroPhaseType phaseType;

    @Column(nullable = false)
    private Integer round;

    public static FocusInterval create(UUID sessionId, LocalDateTime startedAt, PomodoroPhaseType phaseType, Integer round) {
        return new FocusInterval(
                sessionId,
                startedAt,
                null,
                phaseType,
                round
        );
    }

    public void end(LocalDateTime now) {
        this.endedAt = now;
    }
}
