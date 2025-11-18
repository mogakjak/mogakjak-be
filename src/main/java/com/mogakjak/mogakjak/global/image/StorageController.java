package com.mogakjak.mogakjak.global.image;

import com.mogakjak.mogakjak.global.common.ApiResponse;
import com.mogakjak.mogakjak.global.exception.status.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Image", description = "이미지 업로드 관련 API")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @Operation(summary = "Presigned URL 발급", description = "이미지를 업로드할 수 있는 임시 URL을 발급받습니다.")
    @GetMapping("/presigned-url")
    public ApiResponse<PresignedUrlResponse> getPresignedUrl(
            @RequestParam String prefix,
            @RequestParam String fileName
    ) {
        return ApiResponse.success(SuccessCode.OK, storageService.getPresignedUrl(prefix, fileName));
    }
}