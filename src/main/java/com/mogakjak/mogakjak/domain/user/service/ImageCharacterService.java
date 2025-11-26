package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.user.controller.dto.ImageCharacterRequest;
import com.mogakjak.mogakjak.domain.user.controller.dto.ImageCharacterResponse;
import com.mogakjak.mogakjak.domain.user.entity.User;

import java.util.List;
import java.util.UUID;

public interface ImageCharacterService {

    ImageCharacterResponse createCharacter(ImageCharacterRequest request);

    List<ImageCharacterResponse> createCharacters(List<ImageCharacterRequest> requests);

    List<ImageCharacterResponse> getAllCharacters();

    ImageCharacterResponse getCharacter(UUID id);

    ImageCharacterResponse updateCharacter(UUID id, ImageCharacterRequest request);

    void deleteCharacter(UUID id);

    List<ImageCharacterResponse> checkAndAwardCharacters(User user, Long totalStudyTimeInSeconds);
}
