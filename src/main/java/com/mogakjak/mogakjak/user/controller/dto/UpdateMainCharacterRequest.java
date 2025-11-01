package com.mogakjak.mogakjak.user.controller.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateMainCharacterRequest {

    @NotNull(message = "캐릭터 ID는 필수 입력값입니다.")
    private UUID characterId;
}