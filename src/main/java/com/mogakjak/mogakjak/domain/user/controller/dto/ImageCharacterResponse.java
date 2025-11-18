package com.mogakjak.mogakjak.domain.user.controller.dto;

import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ImageCharacterResponse {

    private UUID id;
    private Integer level;
    private String name;
    private String mainCharacterImage;
    private Boolean isActive;
    private Integer unlockTimeInSeconds;

    public static ImageCharacterResponse from(ImageCharacter character) {
        return ImageCharacterResponse.builder()
                .id(character.getId())
                .level(character.getLevel())
                .name(character.getName())
                .mainCharacterImage(character.getImageUrl())
                .isActive(character.getIsActive())
                .unlockTimeInSeconds(character.getUnlockTimeInSeconds())
                .build();
    }
}
