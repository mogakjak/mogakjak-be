package com.mogakjak.mogakjak.domain.lounge.dto;

import jakarta.validation.constraints.NotNull;

public record OfficialLoungeFocusCheckRequest(
        @NotNull Boolean enabled
) {
}
