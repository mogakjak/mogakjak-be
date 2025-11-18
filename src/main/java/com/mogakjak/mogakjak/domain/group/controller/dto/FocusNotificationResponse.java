package com.mogakjak.mogakjak.domain.group.controller.dto;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record FocusNotificationResponse (

        @Schema(description = "그룹 아이디", example="7f000001-9a95-1a07-819a-95eec12a0000")
        UUID groupId,

        @Schema(description = "그룹명", example = "몰딥브")
        String groupName,

        @Schema(description = "알림 기능 사용 여부", example = "true")
        Boolean isNotificationAgreed,

        @Schema(description = "알림 주기(시간 단위)", example = "2")
        Integer notificationCycle,

        @Schema(description = "알림 메시지", example = "집중!!!!!!!! 집중!!!!!!!!!!!!!")
        String notificationMessage
) {
    public static FocusNotificationResponse from(Group group) {
        return new FocusNotificationResponse(
                group.getId(),
                group.getName(),
                group.getIsNotificationAgreed(),
                group.getNotificationCycle(),
                group.getNotificationMessage()
        );
    }
}