package com.mogakjak.mogakjak.domain.user.controller.dto;

import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.service.CharacterGrowthPolicy;
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
    private Long requiredAttendanceDays;

    public static ImageCharacterResponse from(ImageCharacter character) {
        CharacterGrowthPolicy policy = CharacterGrowthPolicy.forLevel(character.getLevel())
                .orElse(CharacterGrowthPolicy.LEVEL_12);
        return ImageCharacterResponse.builder()
                .id(character.getId())
                .level(character.getLevel())
                .name(character.getName())
                .mainCharacterImage(character.getImageUrl())
                .isActive(character.getIsActive())
                .unlockTimeInSeconds((int) policy.requiredFocusSeconds())
                .requiredAttendanceDays(policy.requiredAttendanceDays())
                .build();
    }
}
