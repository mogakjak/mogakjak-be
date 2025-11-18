
package com.mogakjak.mogakjak.domain.user.controller.dto;

import com.mogakjak.mogakjak.domain.quote.dto.QuoteResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyProfileResponse {

    private String nickname;
    private String imageUrl;
    private ImageCharacterResponse character;
    private QuoteResponse quote;
}
