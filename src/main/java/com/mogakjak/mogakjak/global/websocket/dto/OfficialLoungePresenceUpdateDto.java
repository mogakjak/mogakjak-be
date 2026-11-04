package com.mogakjak.mogakjak.global.websocket.dto;

import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeMemberResponse;
import com.mogakjak.mogakjak.domain.lounge.dto.LoungeNpcMemberResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficialLoungePresenceUpdateDto {
    private UUID loungeId;
    private String eventType;
    private UUID changedUserId;
    private Long currentMemberCount;
    private Integer maxMemberCount;
    private List<OfficialLoungeMemberResponse> members;
    @Builder.Default
    private List<LoungeNpcMemberResponse> npcMembers = List.of();
    private Long displayMemberCount;
}
