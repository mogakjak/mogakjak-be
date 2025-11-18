package com.mogakjak.mogakjak.domain.user.controller;

import com.mogakjak.mogakjak.domain.user.controller.dto.*;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.auth.security.resolver.CurrentUser;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import com.mogakjak.mogakjak.domain.user.service.MyPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Tag(name = "MyPage", description = "마이페이지 관련 API")
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @Operation(summary = "내 채소 바구니 조회", description = "마이페이지의 '내 채소 바구니' 탭 정보를 조회합니다.")
    @GetMapping("/character-basket")
    public ApiResponse<CharacterBasketResponse> getCharacterBasket(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);

        CharacterBasketResponse response = myPageService.getCharacterBasket(userId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "프로필 조회", description = "사용자의 프로필을 조회합니다.")
    @GetMapping("/profile")
    public ApiResponse<MyProfileResponse> getProfile(
            @Parameter(hidden = true) @CurrentUser User user
    ) {
        MyProfileResponse response = myPageService.getProfile(user);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "프로필 수정", description = "입력된 닉네임, 이메일, 프로필 이미지 URL로 사용자의 프로필 정보를 수정합니다. 입력된 필드만 수정되며, 입력하지 않은 필드(null)는 기존 값을 유지합니다.")
    @PatchMapping("/profile")
    public ApiResponse<Void> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = getUserId(userDetails);

        myPageService.updateProfile(userId, request);
        return ApiResponse.success(SuccessCode.OK);
    }

    @Operation(summary = "대표 캐릭터 변경", description = "사용자의 대표 캐릭터를 변경합니다.")
    @PatchMapping("/character")
    public ApiResponse<Void> updateMainCharacter(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateMainCharacterRequest request
    ) {
        UUID userId = getUserId(userDetails);

        myPageService.updateMainCharacter(userId, request.getCharacterId());
        return ApiResponse.success(SuccessCode.OK);
    }

    @Operation(summary = "채소 도감 조회", description = "전체 캐릭터 도감 목록과 해금 조건을 조회합니다.")
    @GetMapping("/characters/guide")
    public ApiResponse<List<CharacterGuideResponse>> getCharacterGuide() {
        List<CharacterGuideResponse> response = myPageService.getCharacterGuide();
        return ApiResponse.success(SuccessCode.OK, response);
    }

    private UUID getUserId(CustomUserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}