package com.mogakjak.mogakjak.domain.group.controller.dto;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.user.entity.GroupRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "그룹 상세 정보 응답 DTO")
public class GroupDetailResponse {

    @Schema(description = "그룹 ID")
    private UUID groupId;

    @Schema(description = "그룹 이름")
    private String name;

    @Schema(description = "그룹 이미지 URL")
    private String imageUrl;

    @Schema(description = "그룹 설명")
    private String description;

    @Schema(description = "그룹 타이머 누적 시간 (초 단위)")
    private Long accumulatedDuration;

    @Schema(description = "그룹 공동 목표")
    private GroupGoalResponse groupGoal;

    private Long progressRate;

    @Schema(description = "현재 그룹 세션 참여 인원", example = "3")
    private Long participatingMemberCount;

    @Schema(description = "탈퇴·삭제 사용자를 제외한 전체 메이트 수", example = "8")
    private Long totalMemberCount;

    @Schema(description = "그룹 멤버 목록")
    private List<MemberInfo> members;

    @Getter
    @Builder
    @Schema(description = "그룹 멤버 정보")
    public static class MemberInfo {
        @Schema(description = "유저 ID")
        private UUID userId;
        @Schema(description = "유저 닉네임")
        private String nickname;
        @Schema(description = "유저 프로필 이미지 URL")
        private String profileUrl;
        @Schema(description = "유저 레벨")
        private Integer level;
        @Schema(description = "유저 역할(방장/팀원)")
        private GroupRole role;
    }

    public static GroupDetailResponse from(
            Group group,
            List<MemberInfo> members,
            long participatingMemberCount,
            long totalMemberCount
    ) {
        long accumulated = group.getAccumulatedDuration() != null ? group.getAccumulatedDuration() : 0L;
        long goalSeconds = group.getGoalSeconds() != null ? group.getGoalSeconds() : 0L;

        long progressRate = 0L;
        if (goalSeconds > 0) {
            progressRate = (long) ((double) accumulated / goalSeconds * 100);
        }

        return GroupDetailResponse.builder()
                .groupId(group.getId())
                .name(group.getName())
                .imageUrl(group.getImageUrl())
                .description(group.getDescription())
                .accumulatedDuration(group.getAccumulatedDuration() != null ? group.getAccumulatedDuration() : 0L)
                .groupGoal(GroupGoalResponse.from(group))
                .progressRate(progressRate)
                .participatingMemberCount(participatingMemberCount)
                .totalMemberCount(totalMemberCount)
                .members(members)
                .build();
    }
}
