package com.mogakjak.mogakjak.domain.group.controller.dto;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record GroupGoalResponse(
        @Schema(description = "그룹 아이디", example = "7f000001-9a96-1b00-819a-96eb4d570000")
        UUID groupId,

        @Schema(description = "그룹 공동 목표 - 시간", example = "3600")
        Integer goalHours,

        @Schema(description = "그룹 공동 목표 - 분", example = "3600")
        Integer goalMinutes
) {
        public static GroupGoalResponse from(Group group) {
                int totalSeconds = group.getGoalSeconds();
                int hours = totalSeconds / 3600;
                int minutes = (totalSeconds % 3600) / 60;

                return new GroupGoalResponse(
                        group.getId(),
                        hours,
                        minutes
                );
        }
}
