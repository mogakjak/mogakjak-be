package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FocusNotificationRequest (
        @NotNull @Min(1) @Max(99)
        @Schema(description = "알림 주기(시간 단위)", example = "2")
        Integer notificationCycle
) {}
