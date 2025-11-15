package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.user.controller.dto.*;
import com.mogakjak.mogakjak.domain.user.entity.User;

import java.util.List;
import java.util.UUID;

public interface MyPageService {

    // 내 채소 바구니 정보 조회
    CharacterBasketResponse getCharacterBasket(UUID userId);

    // 프로필 수정
    void updateProfile(UUID userId, UpdateProfileRequest request);

    // 대표 캐릭터 변경
    void updateMainCharacter(UUID userId, UUID characterId);

    // 채소 도감 조회
    List<CharacterGuideResponse> getCharacterGuide();

    // 내 프로필 조회 (홈 용)
    MyProfileResponse getProfile(User user);
}