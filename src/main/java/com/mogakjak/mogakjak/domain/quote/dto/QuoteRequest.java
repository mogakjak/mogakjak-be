package com.mogakjak.mogakjak.domain.quote.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequest {

    @NotBlank
    private String content;

    @NotBlank
    private String author;
}
