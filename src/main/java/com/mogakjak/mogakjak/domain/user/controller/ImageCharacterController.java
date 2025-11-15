package com.mogakjak.mogakjak.domain.user.controller;

import com.mogakjak.mogakjak.domain.user.controller.dto.ImageCharacterRequest;
import com.mogakjak.mogakjak.domain.user.controller.dto.ImageCharacterResponse;
import com.mogakjak.mogakjak.domain.user.service.ImageCharacterService;
import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
