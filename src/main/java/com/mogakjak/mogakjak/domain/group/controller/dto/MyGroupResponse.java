package com.mogakjak.mogakjak.domain.group.controller.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyGroupResponse {
    private UUID groupId;
    private String groupName;
    private String imageUrl;
    private List<GroupMemberDto> members;

    @Getter
    @Builder
    public static class GroupMemberDto {
        private UUID userId;
        private String nickname;
        private String profileUrl;
        private Integer level;
    }
}