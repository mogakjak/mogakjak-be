package com.mogakjak.mogakjak.domain.timer.entity;

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

    // POMODORO일 때만 의미 있는 필드
    // 일반 타이머면 null이거나 NORMAL로 두면 된다
    @Column(name = "type")
    private String type;   // FOCUS | BREAK | NORMAL

    @Column(name = "round")
    private Integer round; // 1, 2, 3 ... 번째 뽀모도로
}
