package com.mogakjak.mogakjak.domain.lounge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficialLoungeMemberResponse {
    private UUID userId;
    private String nickname;
    private String profileUrl;
    private Integer level;
}
