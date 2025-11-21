package com.mogakjak.mogakjak.domain.group.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Schema(description = "콕 찌르기 요청 DTO")
public class PokeRequest {
    
    @NotNull(message = "콕 찌를 사용자 ID는 필수입니다.")
    @Schema(description = "콕 찌를 사용자의 ID")
    private UUID targetUserId;
    
    @NotNull(message = "그룹 ID는 필수입니다.")
    @Schema(description = "콕 찌르기를 보낼 그룹의 ID")
    private UUID groupId;
}

