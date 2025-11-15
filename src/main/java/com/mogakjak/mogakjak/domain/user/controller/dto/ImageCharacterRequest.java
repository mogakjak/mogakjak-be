package com.mogakjak.mogakjak.domain.user.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ImageCharacterRequest {

    @NotNull
    private Integer level;

    @NotBlank
    private String name;

    @NotBlank
    private String imageUrl;

    @NotNull
    private Boolean isActive;

    @NotNull
    private Integer unlockTimeInSeconds;
}
