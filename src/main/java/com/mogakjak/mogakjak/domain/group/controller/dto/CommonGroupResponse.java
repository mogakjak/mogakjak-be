package com.mogakjak.mogakjak.domain.group.controller.dto;

import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "두 사용자가 함께 있는 그룹 정보")
public class CommonGroupResponse {
    @Schema(description = "그룹 ID")
    private UUID groupId;
    
    @Schema(description = "그룹 이름")
    private String groupName;
    
    @Schema(description = "그룹 이미지 URL")
    private String imageUrl;
    
    @Schema(description = "현재 멤버 수")
    private Long memberCount;
    
    @Schema(description = "최대 멤버 수")
    private Integer maxMemberCount;
    
    @Schema(description = "현재 사용자의 그룹 참여 상태")
    private GroupParticipationStatus myParticipationStatus;
    
    @Schema(description = "상대방의 그룹 참여 상태")
    private GroupParticipationStatus targetParticipationStatus;
}

