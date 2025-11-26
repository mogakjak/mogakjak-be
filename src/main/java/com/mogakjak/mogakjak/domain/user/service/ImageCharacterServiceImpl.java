package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.user.controller.dto.ImageCharacterRequest;
import com.mogakjak.mogakjak.domain.user.controller.dto.ImageCharacterResponse;
import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserCharacter;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageCharacterServiceImpl implements ImageCharacterService {

    private final ImageCharacterRepository repository;
    private final UserCharacterRepository userCharacterRepository;

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

    @Override
    @Transactional
    public List<ImageCharacterResponse> checkAndAwardCharacters(User user, Long totalStudyTimeInSeconds) {
        List<ImageCharacter> unlockableCharacters = repository.findAllByUnlockTimeInSecondsLessThanEqual(totalStudyTimeInSeconds.intValue());

        List<UserCharacter> userCharacters = userCharacterRepository.findAllByUser(user);
        Set<ImageCharacter> ownedCharacters = userCharacters.stream()
                .map(UserCharacter::getImageCharacter)
                .collect(Collectors.toSet());

        List<ImageCharacter> newlyAwardedCharacters = unlockableCharacters.stream()
                .filter(character -> !ownedCharacters.contains(character))
                .toList();

        if (!newlyAwardedCharacters.isEmpty()) {
            List<UserCharacter> newUserCharacters = newlyAwardedCharacters.stream()
                    .map(character -> UserCharacter.builder().user(user).imageCharacter(character).build())
                    .toList();
            userCharacterRepository.saveAll(newUserCharacters);
        }

        return newlyAwardedCharacters.stream()
                .map(ImageCharacterResponse::from)
                .toList();
    }
}