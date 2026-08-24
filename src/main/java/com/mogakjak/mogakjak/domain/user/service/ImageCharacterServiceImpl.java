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
    private final CharacterGrowthService characterGrowthService;

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
    public List<ImageCharacterResponse> checkAndAwardCharacters(User user) {
        CharacterGrowthStatus growthStatus = characterGrowthService.getStatus(user.getId());
        List<ImageCharacter> unlockableCharacters = repository.findAllByOrderByLevelAsc().stream()
                .filter(character -> Boolean.TRUE.equals(character.getIsActive()))
                .filter(character -> CharacterGrowthPolicy.forLevel(character.getLevel())
                        .map(policy -> policy.isSatisfiedBy(growthStatus))
                        .orElse(false))
                .toList();

        List<UserCharacter> userCharacters = userCharacterRepository.findAllByUser(user);
        Set<UUID> ownedCharacterIds = userCharacters.stream()
                .map(UserCharacter::getImageCharacter)
                .map(ImageCharacter::getId)
                .collect(Collectors.toSet());

        List<ImageCharacter> newlyAwardedCharacters = unlockableCharacters.stream()
                .filter(character -> !ownedCharacterIds.contains(character.getId()))
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
