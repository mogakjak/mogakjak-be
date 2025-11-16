package com.mogakjak.mogakjak.domain.timer.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class FocusInterval extends BaseSchema {

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    public static FocusInterval create(UUID sessionId, LocalDateTime startedAt) {
        return new FocusInterval(
                sessionId,
                startedAt,
                null
        );
    }

    public void end(LocalDateTime now) {
        this.endedAt = now;
    }
}
