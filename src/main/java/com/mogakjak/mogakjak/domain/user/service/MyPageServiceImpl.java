package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.quote.dto.QuoteResponse;
import com.mogakjak.mogakjak.domain.quote.entity.Quote;
import com.mogakjak.mogakjak.domain.quote.repository.QuoteRepository;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.controller.dto.*;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserProfile;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserProfileRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageServiceImpl implements MyPageService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TodoRepository todoRepository;
    private final ImageCharacterRepository imageCharacterRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final QuoteRepository quoteRepository;

    private static final int DEFAULT_CHARACTER_LEVEL = 1;

    // 내 채소 바구니 정보 조회
    @Override
    public CharacterBasketResponse getCharacterBasket(UUID userId) {
        User user = findUserById(userId);
        UserProfile userProfile = findOrCreateUserProfile(user);

        Long totalTaskCount = todoRepository.countByUserAndIsCompleted(user, true);

        Long totalSeconds = todoRepository.sumActualTimeByUser(user).orElse(0L);
        String formattedTotalTime = formatSecondsToHoursMinutes(totalSeconds);

        List<ImageCharacter> allImageCharacters = imageCharacterRepository.findAll();

        Set<UUID> ownedCharacterIds = userCharacterRepository.findAllByUserWithImageCharacter(user)
                .stream()
                .map(userCharacter -> userCharacter.getImageCharacter().getId())
                .collect(Collectors.toSet());

        // 기본 캐릭터(해금 시간 0초) ID를 보유 목록에 추가합니다.
        allImageCharacters.stream()
                .filter(c -> c.getUnlockTimeInSeconds() == 0)
                .forEach(defaultChar -> ownedCharacterIds.add(defaultChar.getId()));

        List<CharacterBasketResponse.CharacterDto> ownedDtos = allImageCharacters.stream()
                .filter(c -> ownedCharacterIds.contains(c.getId()))
                .map(this::toCharacterDto)
                .collect(Collectors.toList());

        List<CharacterBasketResponse.CharacterDto> lockedDtos = allImageCharacters.stream()
                .filter(c -> !ownedCharacterIds.contains(c.getId()))
                .map(this::toCharacterDto)
                .collect(Collectors.toList());

        ImageCharacter mainCharacterEntity = userProfile.getMainImageCharacter();
        CharacterBasketResponse.CharacterDto mainCharacterDto = null;
        ImageCharacter defaultCharacter = allImageCharacters.stream()
                .filter(c -> c.getUnlockTimeInSeconds() == 0)
                .findFirst()
                .orElse(null);

        if (mainCharacterEntity != null) {
            mainCharacterDto = toCharacterDto(mainCharacterEntity);
        } else if (defaultCharacter != null) {
            // 설정된 대표 캐릭터가 없으면 기본 캐릭터(Lv 1)로 설정
            mainCharacterDto = toCharacterDto(defaultCharacter);
        }

        return CharacterBasketResponse.builder()
                .nickname(user.getName())
                .email(user.getEmail())
                .mainCharacter(mainCharacterDto)
                .totalTaskCount(totalTaskCount)
                .totalFocusTime(formattedTotalTime)
                .collectedCharacterCount(ownedDtos.size())
                .ownedCharacters(ownedDtos)
                .lockedCharacters(lockedDtos)
                .build();
    }

    // 프로필 수정
    @Override
    @Transactional
    public void updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        String newName = user.getName();
        if (StringUtils.hasText(request.getNickname()) && !user.getName().equals(request.getNickname())) {
            if (userRepository.findByName(request.getNickname()).isPresent()) {
                throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
            }
            newName = request.getNickname();
        }

        String newEmail = user.getEmail();
        if (StringUtils.hasText(request.getEmail()) && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            newEmail = request.getEmail();
        }

        // 프로필 이미지 변경 (null이 아닌 경우 업데이트)
        String newImageUrl = request.getImageUrl() != null ? request.getImageUrl() : user.getImageUrl();

        user.updateInfo(newName, newEmail, newImageUrl);
    }

    @Override
    @Transactional
    public void updateMainCharacter(UUID userId, UUID characterId) {
        User user = findUserById(userId);
        ImageCharacter imageCharacter = imageCharacterRepository.findById(characterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));

        boolean isOwned = userCharacterRepository.existsByUserAndImageCharacter(user, imageCharacter);
        if (!isOwned) {
            throw new CustomException(ErrorCode.CHARACTER_NOT_OWNED);
        }

        UserProfile userProfile = findOrCreateUserProfile(user);
        userProfile.updateMainCharacter(imageCharacter);
    }

    // 채소 도감 조회
    @Override
    public List<CharacterGuideResponse> getCharacterGuide() {
        return imageCharacterRepository.findAllByOrderByLevelAsc().stream()
                .map(this::toCharacterGuideDto)
                .collect(Collectors.toList());
    }

    @Override
    public MyProfileResponse getProfile(User user) {

        UserProfile userProfile = findOrCreateUserProfile(user);

        ImageCharacter mainCharacter = userProfile.getMainImageCharacter();
        if (mainCharacter == null) {
            mainCharacter = imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(DEFAULT_CHARACTER_LEVEL)
                    .orElseThrow(() -> new CustomException(ErrorCode.CHARACTER_NOT_FOUND));
        }

        return MyProfileResponse.builder()
                .nickname(user.getName())
                .imageUrl(user.getImageUrl())
                .character(ImageCharacterResponse.from(mainCharacter))
                .quote(QuoteResponse.from(getRandomQuote()))
                .build();
    }

    private Quote getRandomQuote() {
        long count = quoteRepository.count();
        if (count == 0) {
            throw new CustomException(ErrorCode.QUOTE_NOT_FOUND);
        }

        int randomIndex = (int) (Math.random() * count);
        return quoteRepository.findRandomByOffset(randomIndex);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private UserProfile findOrCreateUserProfile(User user) {
        return userProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder()
                            .user(user)
                            .build();
                    return userProfileRepository.save(newProfile);
                });
    }

    private String formatSecondsToHoursMinutes(Long totalSeconds) {
        if (totalSeconds == null || totalSeconds == 0) {
            return "0시간 0분";
        }
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return String.format("%d시간 %d분", hours, minutes);
    }

    private String formatSecondsToUnlockCondition(Integer totalSeconds) {
        long hours = totalSeconds / 3600;
        return String.format("누적 %d시간", hours);
    }

    private CharacterBasketResponse.CharacterDto toCharacterDto(ImageCharacter imageCharacter) {
        return CharacterBasketResponse.CharacterDto.builder()
                .characterId(imageCharacter.getId())
                .name(imageCharacter.getName())
                .imageUrl(imageCharacter.getImageUrl())
                .level(imageCharacter.getLevel())
                .unlockCondition(formatSecondsToUnlockCondition(imageCharacter.getUnlockTimeInSeconds()))
                .build();
    }

    private CharacterGuideResponse toCharacterGuideDto(ImageCharacter imageCharacter) {
        return CharacterGuideResponse.builder()
                .id(imageCharacter.getId())
                .level(imageCharacter.getLevel())
                .name(imageCharacter.getName())
                .imageUrl(imageCharacter.getImageUrl())
                .unlockTime(formatSecondsToUnlockCondition(imageCharacter.getUnlockTimeInSeconds()))
                .build();
    }
}