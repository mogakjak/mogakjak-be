package com.mogakjak.mogakjak.controller.dto.my_page;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CharacterGuideResponse {
    private UUID id;
    private int level;
    private String name;
    private String imageUrl;
    private String unlockTime; // e.g., "5시간"
}