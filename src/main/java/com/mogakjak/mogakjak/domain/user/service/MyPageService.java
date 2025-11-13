package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.user.controller.dto.CharacterBasketResponse;
import com.mogakjak.mogakjak.domain.user.controller.dto.CharacterGuideResponse;
import com.mogakjak.mogakjak.domain.user.controller.dto.UpdateProfileRequest;
import com.mogakjak.mogakjak.domain.user.controller.dto.UserSearchResponse;
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

}