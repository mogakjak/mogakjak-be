package com.mogakjak.mogakjak.global.websocket.dto;

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
public class GroupMemberStatusUpdateDto {
    private UUID groupId;
    private List<GroupMemberStatusDto> members; // 전체 멤버 목록 또는 변경된 멤버만
    private GroupMemberStatusDto updatedMember; // 변경된 멤버 (선택적)
}

