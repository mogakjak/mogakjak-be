package com.mogakjak.mogakjak.domain.lounge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mogakjak.mogakjak.domain.lounge.npc.LoungeNpcType;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "표시 전용 공식 라운지 NPC. 사용자 ID나 실제 집중 기록을 갖지 않습니다.")
public record LoungeNpcMemberResponse(
        String npcId,
        LoungeNpcType npcType,
        String nickname,
        String profileUrl,
        Integer level,
        GroupParticipationStatus participationStatus,
        @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime enteredAt,
        Long personalTimerSeconds,
        String todoTitle
) {}
