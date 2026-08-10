package com.mogakjak.mogakjak.global.websocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeMemberResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfficialLoungePresenceUpdateDto {
    private UUID loungeId;
    private String eventType;
    private UUID changedUserId;
    private Long currentMemberCount;
    private Integer maxMemberCount;
    private Instant publishedAt;
    private List<OfficialLoungeMemberResponse> members;
    private OfficialLoungeMemberResponse changedMember;
}
