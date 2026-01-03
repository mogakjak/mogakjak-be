package com.mogakjak.mogakjak.domain.user.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckOnboardingStatusResponse {
    private Boolean isFirstVisit;
}
