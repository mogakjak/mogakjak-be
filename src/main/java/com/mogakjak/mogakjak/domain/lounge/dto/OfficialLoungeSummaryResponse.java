package com.mogakjak.mogakjak.domain.lounge.dto;

import com.mogakjak.mogakjak.domain.quote.dto.QuoteResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class OfficialLoungeSummaryResponse {
    private UUID loungeId;
    private String loungeName;
    private String imageUrl;
    private Long currentMemberCount;
    private Integer maxMemberCount;
    private Boolean hasEntered;
    private Boolean myFocusCheckEnabled;
    private QuoteResponse todayQuote;
    private List<OfficialLoungeMemberResponse> members;
}
