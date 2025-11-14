package com.mogakjak.mogakjak.domain.user.controller;

import com.mogakjak.mogakjak.domain.user.controller.dto.MemberListResDto;
import com.mogakjak.mogakjak.domain.user.controller.dto.UserSearchResponse;
import com.mogakjak.mogakjak.domain.user.service.UserService;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "User", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
}