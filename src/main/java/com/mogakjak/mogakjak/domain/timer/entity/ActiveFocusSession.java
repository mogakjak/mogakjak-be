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
public class ActiveFocusSession extends BaseSchema {

    @Column(nullable = false, unique = true)
    private UUID sessionId;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    public static ActiveFocusSession create(UUID sessionId, UUID userId, LocalDateTime startedAt) {
        return new ActiveFocusSession(
                sessionId,
                userId,
                startedAt
        );
    }
}
