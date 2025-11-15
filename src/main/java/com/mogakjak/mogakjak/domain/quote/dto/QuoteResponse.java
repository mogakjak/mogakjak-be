package com.mogakjak.mogakjak.domain.quote.dto;

import com.mogakjak.mogakjak.domain.quote.entity.Quote;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuoteResponse {

    private UUID id;
    private String content;
    private String author;

    public static QuoteResponse from(Quote quote) {
        return QuoteResponse.builder()
                .id(quote.getId())
                .content(quote.getContent())
                .author(quote.getAuthor())
                .build();
    }
}
