package com.mogakjak.mogakjak.domain.timer.entity;

import com.mogakjak.mogakjak.domain.timer.enumerate.IntervalType;
import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimerInterval extends BaseSchema {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Column(name = "type")
    private IntervalType type;   // FOCUS | BREAK - 뽀모도로 / NORMAL - 일반

    @Column(name = "round")
    private Integer round; // 1, 2, 3 ... 번째 뽀모도로
}
