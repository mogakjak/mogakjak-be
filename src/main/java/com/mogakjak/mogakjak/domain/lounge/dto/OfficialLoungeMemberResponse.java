package com.mogakjak.mogakjak.domain.lounge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficialLoungeMemberResponse {
    private UUID userId;
    private String nickname;
    private String profileUrl;
    private Integer level;
    private String participationStatus;
    private LocalDateTime enteredAt;
    private LocalDateTime lastActiveAt;
    private Long daysSinceLastParticipation;
    private Long personalTimerSeconds; // 현재 Todo의 누적 몰입 시간 (실행 중 구간 포함, 비공개면 null)
    private String todoTitle;
    private Integer cheerCount;
    private Boolean isMate;
}
