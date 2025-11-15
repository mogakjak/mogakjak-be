package com.mogakjak.mogakjak.domain.quote.service;

import com.mogakjak.mogakjak.domain.quote.dto.QuoteRequest;
import com.mogakjak.mogakjak.domain.quote.dto.QuoteResponse;
import com.mogakjak.mogakjak.domain.quote.entity.Quote;

import com.mogakjak.mogakjak.domain.quote.repository.QuoteRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {

    private final QuoteRepository quoteRepository;

    @Transactional
    public QuoteResponse createQuote(QuoteRequest request) {
        Quote quote = Quote.builder()
                .content(request.getContent())
                .author(request.getAuthor())
                .build();

        quoteRepository.save(quote);

        return QuoteResponse.from(quote);
    }

    public List<QuoteResponse> getAllQuotes() {
        return quoteRepository.findAll()
                .stream()
                .map(QuoteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuoteResponse getQuote(UUID id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.QUOTE_NOT_FOUND));

        return QuoteResponse.from(quote);
    }

    @Transactional
    public QuoteResponse updateQuote(UUID id, QuoteRequest request) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.QUOTE_NOT_FOUND));

        quote.update(request.getContent(), request.getAuthor());

        return QuoteResponse.from(quote);
    }

    @Transactional
    public void deleteQuote(UUID id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.QUOTE_NOT_FOUND));

        quoteRepository.delete(quote);
    }

    @Transactional
    public List<QuoteResponse> createQuotes(List<QuoteRequest> requests) {
        List<Quote> quotes = requests.stream()
                .map(req -> Quote.builder()
                        .content(req.getContent())
                        .author(req.getAuthor())
                        .build())
                .toList();

        List<Quote> saved = quoteRepository.saveAll(quotes);

        return saved.stream()
                .map(QuoteResponse::from)
                .toList();
    }

}
