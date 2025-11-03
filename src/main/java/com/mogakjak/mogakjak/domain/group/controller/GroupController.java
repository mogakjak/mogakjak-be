package com.mogakjak.mogakjak.domain.group.controller;

import com.mogakjak.mogakjak.domain.group.controller.dto.MyGroupResponse;
import com.mogakjak.mogakjak.domain.group.service.GroupService;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "내 그룹 목록 조회", description = "사용자가 속한 그룹 목록을 조회합니다.")
    @GetMapping("/my")
    public ApiResponse<List<MyGroupResponse>> getMyGroups(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID userId = getUserId(userDetails);

        List<MyGroupResponse> response = groupService.getMyGroups(userId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    private UUID getUserId(CustomUserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}