package com.mogakjak.mogakjak.domain.lounge.controller;

import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeSummaryResponse;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeFocusCheckRequest;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungeService;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Official Lounge", description = "모각작 공식 라운지 API")
@RestController
@RequestMapping("/api/lounge")
@RequiredArgsConstructor
public class OfficialLoungeController {

    private final OfficialLoungeService officialLoungeService;

    @Operation(summary = "공식 라운지 현황 조회")
    @GetMapping
    public ApiResponse<OfficialLoungeSummaryResponse> getLoungeSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        return ApiResponse.success(SuccessCode.OK, officialLoungeService.getSummary(userId));
    }

    @Operation(summary = "공식 라운지 입실")
    @PostMapping("/enter")
    public ApiResponse<OfficialLoungeSummaryResponse> enterLounge(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        return ApiResponse.success(SuccessCode.OK, officialLoungeService.enter(userId));
    }

    @Operation(summary = "공식 라운지 퇴실")
    @DeleteMapping("/leave")
    public ApiResponse<OfficialLoungeSummaryResponse> leaveLounge(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        return ApiResponse.success(SuccessCode.OK, officialLoungeService.leave(userId));
    }

    @Operation(summary = "공식 라운지 집중 체크 설정 변경")
    @PutMapping("/focus-check")
    public ApiResponse<OfficialLoungeSummaryResponse> updateFocusCheck(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OfficialLoungeFocusCheckRequest request
    ) {
        UUID userId = getUserId(userDetails);
        return ApiResponse.success(SuccessCode.OK, officialLoungeService.updateFocusCheck(userId, request.enabled()));
    }

    private UUID getUserId(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return UUID.fromString(userDetails.getUsername());
    }
}
