package com.mogakjak.mogakjak.domain.user.controller.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class CharacterBasketResponse {

    private String nickname;
    private String email;
    private Long totalTaskCount;
    private String totalFocusTime;
    private String imageUrl;
    private CharacterGrowthProgressResponse growthProgress;

    private CharacterDto mainCharacter;

    // 수집 캐릭터 수
    private int collectedCharacterCount;

    // 열린 캐릭터 목록
    private List<CharacterDto> ownedCharacters;

    // 잠금 캐릭터 목록
    private List<CharacterDto> lockedCharacters;

    @Getter
    @Builder
    public static class CharacterDto {
        private UUID characterId;
        private String name;
        private String imageUrl;
        private int level;
        private String unlockCondition; // e.g., "누적 200시간"
        private long requiredAttendanceDays;
        private long requiredFocusTimeInSeconds;
        private int attendanceProgressRate;
        private int focusTimeProgressRate;
    }
}
