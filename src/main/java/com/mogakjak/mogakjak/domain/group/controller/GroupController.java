package com.mogakjak.mogakjak.domain.group.controller;

import com.mogakjak.mogakjak.domain.group.controller.dto.*;
import com.mogakjak.mogakjak.domain.group.service.GroupService;
import com.mogakjak.mogakjak.domain.invitation.controller.dto.InvitationUrlResponse;
import com.mogakjak.mogakjak.domain.invitation.controller.dto.InviteMateRequest;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.auth.security.resolver.CurrentUser;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Group", description = "그룹 관련 API")
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Value("${frontend.vercel-url}")
    private String frontendBaseUrl;

    @Operation(summary = "신규 그룹 생성", description = "새로운 스터디 그룹을 생성합니다.")
    @PostMapping
    public ApiResponse<GroupDetailResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        GroupDetailResponse response = groupService.createGroup(request, userId);
        return ApiResponse.success(SuccessCode.CREATED, response);
    }

    @Operation(summary = "내 그룹 목록 조회", description = "현재 로그인한 사용자가 '방장' 또는 '멤버'로 속한 모든 그룹의 목록을 조회합니다.")
    @GetMapping("/my")
    public ApiResponse<List<MyGroupResponse>> getMyGroups(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        List<MyGroupResponse> response = groupService.getMyGroups(userId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "그룹 상세 정보 조회", description = "특정 그룹의 상세 정보와 해당 그룹에 속한 모든 멤버의 목록(닉네임, 역할 등)을 조회합니다. <br> 그룹 멤버가 아닌 경우 403 Forbidden 에러가 발생합니다.")
    @GetMapping("/{groupId}")
    public ApiResponse<GroupDetailResponse> getGroupDetail(
            @Parameter(description = "조회할 그룹의 ID (UUID)", required = true)
            @PathVariable UUID groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        GroupDetailResponse response = groupService.getGroupDetail(groupId, userId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "그룹 정보 수정", description = "그룹의 이름, 설명을 수정합니다.")
    @PutMapping("/{groupId}")
    public ApiResponse<GroupDetailResponse> updateGroup(
            @Parameter(description = "수정할 그룹의 ID (UUID)", required = true)
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateGroupRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        GroupDetailResponse response = groupService.updateGroup(groupId, request, userId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "그룹 집중 체크 알림 설정", description = "그룹의 집중 체크 알림 동의 여부 / 알림 주기 / 알림 메시지를 설정합니다.")
    @PutMapping("/{groupId}/notifications")
    public ApiResponse<FocusNotificationResponse> modifyFocusNotification(
            @Valid @RequestBody FocusNotificationRequest request,
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID groupId
    ) {
        FocusNotificationResponse response = groupService.modifyFocusNotification(user, groupId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "[테스트용] 집중 체크 알림 수동 전송", description = "특정 그룹에 집중 체크 알림을 즉시 전송합니다. (테스트/디버깅용)")
    @PostMapping("/{groupId}/notifications/test")
    public ApiResponse<String> testSendNotification(
            @Parameter(description = "알림을 전송할 그룹의 ID (UUID)", required = true)
            @PathVariable UUID groupId,
            @Parameter(hidden = true) @CurrentUser User user
    ) {
        groupService.testSendFocusNotification(user, groupId);
        return ApiResponse.success(SuccessCode.OK, "알림이 전송되었습니다.");
    }

    @Operation(summary = "그룹 공동 목표 설정", description = "그룹원들이 다같이 달성할 일일 목표 시간을 설정합니다.")
    @PutMapping("/{groupId}/goals")
    public ApiResponse<GroupGoalResponse> modifyGroupGoal(
            @Parameter(hidden = true) @CurrentUser User user,
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupGoalRequest request
    ) {
        GroupGoalResponse response = groupService.setGroupGoal(user, groupId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    // --- Member API ---

    @Operation(summary = "메이트 조회 (전체/그룹별)",
            description = "내 전체 메이트 또는 특정 그룹의 메이트를 페이지네이션으로 조회합니다. <br>" +
                    "  - groupId 생략 시: '내 전체 메이트' (내가 속한 모든 그룹의 멤버)를 조회합니다."
    )
    @GetMapping("/mates")
    public ApiResponse<Page<MateResponse>> getMates(
            @Parameter(description = "특정 그룹 조회 시 사용할 그룹 ID (생략 시 전체 메이트 조회)")
            @RequestParam(required = false) UUID groupId,

            @Parameter(description = "검색할 메이트 닉네임(이름)")
            @RequestParam(required = false) String search,

            @Parameter(hidden = true)
            @PageableDefault(size = 10) Pageable pageable,

            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        Page<MateResponse> response = groupService.getMates(userId, groupId, search, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @Operation(summary = "그룹 탈퇴", description = "현재 로그인한 사용자가 속해있는 그룹에서 탈퇴합니다. <br> - 멤버가 탈퇴하면: 정상적으로 탈퇴 처리됩니다. <br> - 방장이 탈퇴하면: 그룹에 다른 멤버가 있을 경우 탈퇴가 거부됩니다. (400 Bad Request)")
    @DeleteMapping("/{groupId}/members/me")
    public ApiResponse<Void> leaveGroup(
            @Parameter(description = "탈퇴할 그룹의 ID (UUID)", required = true)
            @PathVariable UUID groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        groupService.leaveGroup(groupId, userId);
        return ApiResponse.success(SuccessCode.OK);
    }


    // --- Invitation API ---

    @Operation(summary = "그룹으로 메이트 초대", description = "다른 사용자를 그룹에 초대합니다. <br> 초대받은 사용자는 '초대 수락' API를 호출하기 전까지 'PENDING' 상태가 됩니다.")
    @PostMapping("/{groupId}/invitations")
    public ApiResponse<Void> inviteMate(
            @Parameter(description = "초대할 그룹의 ID (UUID)", required = true)
            @PathVariable UUID groupId,
            @Valid @RequestBody InviteMateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID inviterId = getUserId(userDetails);
        groupService.inviteMate(groupId, request, inviterId);
        return ApiResponse.success(SuccessCode.CREATED);
    }

    private UUID getUserId(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return UUID.fromString(userDetails.getUsername());
    }

    @Operation(summary = "초대 링크로 그룹 가입", description = "공유받은 초대 링크를 통해 그룹에 바로 가입합니다.")
    @PostMapping("/{groupId}/join")
    public ApiResponse<Void> joinGroup(
            @Parameter(description = "가입할 그룹 ID", required = true)
            @PathVariable UUID groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);
        groupService.joinGroupViaLink(groupId, userId);
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
}