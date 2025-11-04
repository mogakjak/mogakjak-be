package com.mogakjak.mogakjak.domain.timer.entity;

import com.mogakjak.mogakjak.domain.timer.enumerate.TimerMode;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class TimerSession extends BaseSchema {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    private TimerMode mode;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    private Long targetDuration;
    private Long totalDuration;

    // 뽀모도로 용
    private Long focusDuration;
    private Long breakDuration;
    private Integer repeatCount;

    @Enumerated(EnumType.STRING)
    private TimerStatus status;
}
