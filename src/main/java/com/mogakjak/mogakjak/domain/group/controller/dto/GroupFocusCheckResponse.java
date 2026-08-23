package com.mogakjak.mogakjak.domain.group.controller.dto;

import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record GroupFocusCheckResponse(
        @Schema(description = "그룹 아이디")
        UUID groupId,

        @Schema(description = "내 그룹 집중 체크 알림 수신 여부", example = "true")
        Boolean myFocusCheckEnabled
) {
    public static GroupFocusCheckResponse from(UserGroup userGroup) {
        return new GroupFocusCheckResponse(
                userGroup.getGroup().getId(),
                userGroup.getIsFocusCheckEnabled()
        );
    }
}
