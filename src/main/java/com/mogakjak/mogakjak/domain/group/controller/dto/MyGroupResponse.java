package com.mogakjak.mogakjak.domain.group.controller.dto;

import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeMemberResponse;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeSummaryResponse;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import java.util.List;
import java.util.UUID;

import com.mogakjak.mogakjak.domain.user.entity.GroupRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyGroupResponse {
    private UUID groupId;
    private String groupName;
    private String imageUrl;
    private Boolean isOfficialLounge;
    private Long currentMemberCount;
    private Integer maxMemberCount;
    private Boolean hasEntered;
    private Boolean myFocusCheckEnabled;
    private List<GroupMemberDto> members;

    @Getter
    @Builder
    public static class GroupMemberDto {
        private UUID userId;
        private String nickname;
        private String profileUrl;
        private Integer level;
        private GroupRole role;
        private GroupParticipationStatus participationStatus;
        private LocalDateTime enteredAt;
        private LocalDateTime lastActiveAt;
        private Long daysSinceLastParticipation;
        private Long personalTimerSeconds;
        private String todoTitle;
        private Integer cheerCount;
        private Boolean isMate;
    }

    public static MyGroupResponse fromGroup(UUID groupId, String groupName, String imageUrl, List<GroupMemberDto> members) {
        return MyGroupResponse.builder()
                .groupId(groupId)
                .groupName(groupName)
                .imageUrl(imageUrl)
                .isOfficialLounge(false)
                .members(members)
                .build();
    }

    public static MyGroupResponse fromOfficialLounge(OfficialLoungeSummaryResponse summary) {
        List<GroupMemberDto> loungeMembers = summary.getMembers() == null
                ? List.of()
                : summary.getMembers().stream()
                .map(MyGroupResponse::toMemberDto)
                .toList();

        return MyGroupResponse.builder()
                .groupId(summary.getLoungeId())
                .groupName(summary.getLoungeName())
                .imageUrl(summary.getImageUrl())
                .isOfficialLounge(true)
                .currentMemberCount(summary.getCurrentMemberCount())
                .maxMemberCount(summary.getMaxMemberCount())
                .hasEntered(summary.getHasEntered())
                .myFocusCheckEnabled(summary.getMyFocusCheckEnabled())
                .members(loungeMembers)
                .build();
    }

    private static GroupMemberDto toMemberDto(OfficialLoungeMemberResponse member) {
        return GroupMemberDto.builder()
                .userId(member.getUserId())
                .nickname(member.getNickname())
                .profileUrl(member.getProfileUrl())
                .level(member.getLevel())
                .role(null)
                .participationStatus(member.getParticipationStatus() == null
                        ? null
                        : GroupParticipationStatus.valueOf(member.getParticipationStatus()))
                .enteredAt(member.getEnteredAt())
                .lastActiveAt(member.getLastActiveAt())
                .daysSinceLastParticipation(member.getDaysSinceLastParticipation())
                .personalTimerSeconds(member.getPersonalTimerSeconds())
                .todoTitle(member.getTodoTitle())
                .cheerCount(member.getCheerCount())
                .isMate(member.getIsMate())
                .build();
    }
}
