package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.user.controller.dto.ImageCharacterRequest;
import com.mogakjak.mogakjak.domain.user.controller.dto.ImageCharacterResponse;
import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageCharacterServiceImpl implements ImageCharacterService {

    private final ImageCharacterRepository repository;

    @Override
    @Transactional
    public ImageCharacterResponse createCharacter(ImageCharacterRequest request) {
        ImageCharacter character = ImageCharacter.builder()
                .level(request.getLevel())
                .name(request.getName())
                .imageUrl(request.getImageUrl())
                .isActive(request.getIsActive())
                .unlockTimeInSeconds(request.getUnlockTimeInSeconds())
                .build();

        return ImageCharacterResponse.from(repository.save(character));
    }

    @Override
    @Transactional
    public List<ImageCharacterResponse> createCharacters(List<ImageCharacterRequest> requests) {
        return requests.stream()
                .map(this::createCharacter)
                .toList();
    }

    @Override
    public List<ImageCharacterResponse> getAllCharacters() {
        return repository.findAll().stream()
                .map(ImageCharacterResponse::from)
                .toList();
    }

    @Override
    public ImageCharacterResponse getCharacter(UUID id) {
        ImageCharacter character = repository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));
        return ImageCharacterResponse.from(character);
    }

    @Override
    @Transactional
    public ImageCharacterResponse updateCharacter(UUID id, ImageCharacterRequest request) {
        ImageCharacter character = repository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        character.update(
                request.getLevel(),
                request.getName(),
                request.getImageUrl(),
                request.getIsActive(),
                request.getUnlockTimeInSeconds()
        );

        return ImageCharacterResponse.from(character);
    }

    @Override
    @Transactional
    public void deleteCharacter(UUID id) {
        if (!repository.existsById(id)) {
            throw new CustomException(ErrorCode.CHARACTER_NOT_FOUND);
        }
        repository.deleteById(id);
    }
}
