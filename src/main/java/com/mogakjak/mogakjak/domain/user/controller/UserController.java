package com.mogakjak.mogakjak.domain.user.controller;

import com.mogakjak.mogakjak.domain.user.controller.dto.*;
import com.mogakjak.mogakjak.domain.user.service.UserService;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "개인 정보 동의", description = "개인 정보 동의 항목들에 동의 여부를 표시합니다.")
    @PostMapping("/agreements")
    public ApiResponse<Void> agreeTerms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AgreementRequest request
    ) {
        userService.agreeTerms(getUserId(userDetails), request);
        return ApiResponse.success(SuccessCode.OK);
    }

    @Operation(summary = "초대할 사용자 검색", description = "닉네임으로 사용자를 검색합니다.")
    @GetMapping("/search")
    public ApiResponse<List<UserSearchResponse>> searchUsers(
            @RequestParam("nickname") String nickname
    ) {
        List<UserSearchResponse> response = userService.searchUsers(nickname);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping("/list")
    public ApiResponse<List<MemberListResDto>> memberList(){
        List<MemberListResDto> dtos = userService.findAll();
        return ApiResponse.success(SuccessCode.OK, dtos);
    }

    @Operation(summary = "신규 사용자 확인 및 온보딩 완료 처리",
            description = "사용자의 첫 로그인 여부를 확인하고, 온보딩 상태를 완료(true)로 업데이트합니다.")
    @PostMapping("/onboarding")
    public ApiResponse<CheckOnboardingStatusResponse> checkOnboardingStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);

        // 서비스에서 '첫 방문이면 true'를 반환
        boolean isFirstVisit = userService.checkFirstVisitAndMarkOnboarded(userId);
        CheckOnboardingStatusResponse response = CheckOnboardingStatusResponse.builder()
                .isFirstVisit(isFirstVisit)
                .build();

        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "계정 탈퇴", description = "유저 계정을 탈퇴합니다.")
    @DeleteMapping("/withdrawal")
    public ApiResponse<String> withdrawUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        userService.deleteUser(getUserId(userDetails));
        return ApiResponse.success(SuccessCode.OK, "계정 탈퇴가 완료되었습니다.");
    }

    private UUID getUserId(CustomUserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}