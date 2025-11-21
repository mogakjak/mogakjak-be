package com.mogakjak.mogakjak.global.websocket.dto;

import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberStatusDto {
    private UUID groupId;
    private UUID userId;
    private String nickname;
    private String profileUrl;
    private Integer level;
    private GroupParticipationStatus participationStatus;
    private LocalDateTime enteredAt;
    private Long daysSinceLastParticipation; // 최근 참여 며칠 전 (null이면 참여한 적 없음)
    private Long personalTimerSeconds; // 개인 타이머 경과 시간 (초 단위, null이면 타이머 실행 중이 아님)
    private String todoTitle; // 현재 실행 중인 할 일 제목 (null이면 타이머 실행 중이 아님)
    private Integer cheerCount; // 응원 수
}

