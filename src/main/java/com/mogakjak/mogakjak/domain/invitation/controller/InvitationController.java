package com.mogakjak.mogakjak.domain.invitation.controller;

import com.mogakjak.mogakjak.domain.group.service.GroupService;
import com.mogakjak.mogakjak.domain.invitation.controller.dto.InvitationResponse;
import com.mogakjak.mogakjak.domain.invitation.controller.dto.InvitationUrlResponse;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Invitation", description = "초대 관련 API")
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final GroupService groupService;

    @Value("${frontend.vercel-url}")
    private String frontendBaseUrl;

    @Operation(summary = "내가 받은 초대 목록 조회", description = "현재 로그인한 사용자가 받은 초대 중, 아직 수락/거절하지 않은 'PENDING' 상태의 초대 목록만 조회합니다.")
    @GetMapping("/my")
    public ApiResponse<List<InvitationResponse>> getMyInvitations(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        List<InvitationResponse> response = groupService.getMyInvitations(userId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "초대 수락", description = "받은 초대를 수락하고 해당 그룹의 '멤버(MEMBER)'가 됩니다.")
    @PostMapping("/{invitationId}/accept")
    public ApiResponse<Void> acceptInvitation(
            @Parameter(description = "수락할 초대의 ID (UUID)", required = true)
            @PathVariable UUID invitationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        groupService.acceptInvitation(invitationId, userId);
        return ApiResponse.success(SuccessCode.OK);
    }

    @Operation(summary = "초대 거절", description = "받은 초대를 거절합니다. (초대 상태가 'DECLINED'로 변경됩니다)")
    @PostMapping("/{invitationId}/decline")
    public ApiResponse<Void> declineInvitation(
            @Parameter(description = "거절할 초대의 ID (UUID)", required = true)
            @PathVariable UUID invitationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        groupService.declineInvitation(invitationId, userId);
        return ApiResponse.success(SuccessCode.OK);
    }

    @Operation(summary = "초대 링크 생성", description = "그룹 초대를 위한 링크를 생성하여 반환합니다.")
    @PostMapping("/{groupId}/url")
    public ApiResponse<InvitationUrlResponse> createInvitationUrl(
            @Parameter(description = "초대 링크 생성할 그룹 ID (UUID)")
            @PathVariable UUID groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        String invitationUrl = groupService.createInvitationUrl(groupId, userId, frontendBaseUrl);

        return ApiResponse.success(SuccessCode.OK, InvitationUrlResponse.builder()
                .groupId(groupId)
                .invitationUrl(invitationUrl)
                .build());
    }

    private UUID getUserId(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return UUID.fromString(userDetails.getUsername());
    }
}