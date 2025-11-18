package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FocusNotificationRequest (

        @NotNull
        @Schema(description = "알림 기능 사용 여부", example = "true")
        Boolean isNotificationAgreed,

        @NotNull @Min(1) @Max(23)
        @Schema(description = "알림 주기(시간 단위)", example = "2")
        Integer notificationCycle,

        @Schema(description = "알림 메시지", example = "집중!!!!!!!! 집중!!!!!!!!!!!!!")
        String notificationMessage

) {}
