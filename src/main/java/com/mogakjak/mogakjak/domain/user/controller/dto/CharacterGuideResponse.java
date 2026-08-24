package com.mogakjak.mogakjak.domain.user.controller.dto;

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
    private long currentAttendanceDays;
    private long currentFocusTimeInSeconds;
    private long requiredAttendanceDays;
    private long requiredFocusTimeInSeconds;
    private boolean unlocked;
    private boolean requirementsSatisfied;
    private int attendanceProgressRate;
    private int focusTimeProgressRate;
}
