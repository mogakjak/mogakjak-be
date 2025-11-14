package com.mogakjak.mogakjak.domain.user.controller.dto;

import com.mogakjak.mogakjak.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@Schema(description = "사용자 검색 결과 응답 DTO")
public class UserSearchResponse {

    @Schema(description = "유저 ID")
    private UUID userId;

    @Schema(description = "닉네임")
    private String nickname;

    public static UserSearchResponse from(User user) {
        return UserSearchResponse.builder()
                .userId(user.getId())
                .nickname(user.getName())
                .build();
    }
}