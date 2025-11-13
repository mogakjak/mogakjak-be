package com.mogakjak.mogakjak.domain.invitation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Schema(description = "그룹 메이트 초대 요청 DTO")
public class InviteMateRequest {

    @NotNull(message = "초대할 유저 ID는 필수입니다.")
    @Schema(description = "초대할 유저의 ID")
    private UUID inviteeId;
}