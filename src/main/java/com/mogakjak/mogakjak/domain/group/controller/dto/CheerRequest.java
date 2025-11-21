package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Schema(description = "응원 보내기 요청 DTO")
public class CheerRequest {
    
    @NotNull(message = "응원을 보낼 사용자 ID는 필수입니다.")
    @Schema(description = "응원을 보낼 사용자의 ID")
    private UUID targetUserId;
}

