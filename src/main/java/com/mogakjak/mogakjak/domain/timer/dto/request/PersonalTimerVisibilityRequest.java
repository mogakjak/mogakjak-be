package com.mogakjak.mogakjak.domain.timer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "개인 타이머 공개/비공개 설정 요청")
public class PersonalTimerVisibilityRequest {
    @Schema(description = "할일 제목 공개 여부", example = "true")
    private Boolean isTaskPublic;

    @Schema(description = "타이머 누적 시간 공개 여부", example = "true")
    private Boolean isTimerPublic;
}

