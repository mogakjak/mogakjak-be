package com.mogakjak.mogakjak.domain.quote.controller;

import com.mogakjak.mogakjak.domain.quote.dto.QuoteRequest;
import com.mogakjak.mogakjak.domain.quote.dto.QuoteResponse;
import com.mogakjak.mogakjak.domain.quote.service.QuoteService;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Quote", description = "명언(Quote) 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    @Operation(summary = "명언 생성", description = "하나의 명언을 새로 생성합니다.")
    @PostMapping
    public ApiResponse<QuoteResponse> createQuote(@RequestBody @Valid QuoteRequest request) {
        return ApiResponse.success(SuccessCode.OK, quoteService.createQuote(request));
    }

    @Operation(summary = "명언 일괄 생성(Bulk)", description = "여러 개의 명언을 한 번에 등록합니다.")
    @PostMapping("/bulk")
    public ApiResponse<List<QuoteResponse>> createQuotes(
            @RequestBody @Valid List<QuoteRequest> requests
    ) {
        return ApiResponse.success(SuccessCode.OK, quoteService.createQuotes(requests));
    }

    @Operation(summary = "전체 명언 조회", description = "등록된 모든 명언을 조회합니다.")
    @GetMapping
    public ApiResponse<List<QuoteResponse>> getAllQuotes() {
        return ApiResponse.success(SuccessCode.OK, quoteService.getAllQuotes());
    }

    @Operation(summary = "명언 단건 조회", description = "특정 명언을 조회합니다.")
    @GetMapping("/{id}")
    public ApiResponse<QuoteResponse> getQuote(@PathVariable UUID id) {
        return ApiResponse.success(SuccessCode.OK, quoteService.getQuote(id));
    }

    @Operation(summary = "명언 수정", description = "특정 명언을 수정합니다.")
    @PutMapping("/{id}")
    public ApiResponse<QuoteResponse> updateQuote(
            @PathVariable UUID id,
            @RequestBody @Valid QuoteRequest request
    ) {
        return ApiResponse.success(SuccessCode.OK, quoteService.updateQuote(id, request));
    }

    @Operation(summary = "명언 삭제", description = "특정 명언을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteQuote(@PathVariable UUID id) {
        quoteService.deleteQuote(id);
        return ApiResponse.success(SuccessCode.OK);
    }
}
