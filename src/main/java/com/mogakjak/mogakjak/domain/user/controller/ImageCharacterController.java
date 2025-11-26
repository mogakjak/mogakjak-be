package com.mogakjak.mogakjak.domain.user.controller;

import com.mogakjak.mogakjak.domain.user.controller.dto.CheckAwardRequest;
import com.mogakjak.mogakjak.domain.user.controller.dto.ImageCharacterRequest;
import com.mogakjak.mogakjak.domain.user.controller.dto.ImageCharacterResponse;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.service.ImageCharacterService;
import com.mogakjak.mogakjak.global.auth.security.CustomUserDetails;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Character", description = "캐릭터 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/characters")
public class ImageCharacterController {

    private final ImageCharacterService service;

    @Operation(summary = "캐릭터 생성")
    @PostMapping
    public ApiResponse<ImageCharacterResponse> create(@RequestBody @Valid ImageCharacterRequest request) {
        return ApiResponse.success(SuccessCode.OK, service.createCharacter(request));
    }

    @Operation(summary = "캐릭터 일괄 생성(Bulk)")
    @PostMapping("/bulk")
    public ApiResponse<List<ImageCharacterResponse>> createBulk(@RequestBody @Valid List<ImageCharacterRequest> requests) {
        return ApiResponse.success(SuccessCode.OK, service.createCharacters(requests));
    }

    @Operation(summary = "누적 시간에 따른 캐릭터 획득 확인")
    @PostMapping("/check-award")
    public ApiResponse<List<ImageCharacterResponse>> checkAndAwardCharacters(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CheckAwardRequest request
    ) {
        User user = userDetails.getUser();
        return ApiResponse.success(SuccessCode.OK, service.checkAndAwardCharacters(user, request.getTotalStudyTimeInSeconds()));
    }

    @Operation(summary = "전체 캐릭터 조회")
    @GetMapping
    public ApiResponse<List<ImageCharacterResponse>> getAll() {
        return ApiResponse.success(SuccessCode.OK, service.getAllCharacters());
    }

    @Operation(summary = "캐릭터 상세 조회")
    @GetMapping("/{id}")
    public ApiResponse<ImageCharacterResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(SuccessCode.OK, service.getCharacter(id));
    }

    @Operation(summary = "캐릭터 수정")
    @PutMapping("/{id}")
    public ApiResponse<ImageCharacterResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid ImageCharacterRequest request
    ) {
        return ApiResponse.success(SuccessCode.OK, service.updateCharacter(id, request));
    }

    @Operation(summary = "캐릭터 삭제")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.deleteCharacter(id);
        return ApiResponse.success(SuccessCode.OK);
    }
}
