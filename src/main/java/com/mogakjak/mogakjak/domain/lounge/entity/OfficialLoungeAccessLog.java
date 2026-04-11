package com.mogakjak.mogakjak.domain.lounge.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficialLoungeAccessLog extends BaseSchema {

    @Column(nullable = false)
    private UUID loungeId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(nullable = false)
    private Long currentMemberCount;

    @Column(nullable = false)
    private Integer maxMemberCount;
}
