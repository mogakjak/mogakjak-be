package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.user.controller.dto.ImageCharacterResponse;
import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserCharacter;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyList;

@ExtendWith(MockitoExtension.class)
class ImageCharacterServiceImplTest {

    @Mock private ImageCharacterRepository repository;
    @Mock private UserCharacterRepository userCharacterRepository;
    @Mock private CharacterGrowthService characterGrowthService;

    @InjectMocks
    private ImageCharacterServiceImpl service;

    @Test
    void checkAndAwardCharacters_usesServerCalculatedLifetimeFocusTime() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().name("user").email("user@example.com").build();
        ReflectionTestUtils.setField(user, "id", userId);
        ImageCharacter character = ImageCharacter.builder()
                .level(2)
                .name("level-2")
                .imageUrl("https://example.com/2.png")
                .isActive(true)
                .unlockTimeInSeconds(7_200)
                .build();
        when(characterGrowthService.getStatus(userId))
                .thenReturn(new CharacterGrowthStatus(15, 20L * 3600));
        when(repository.findAllByOrderByLevelAsc()).thenReturn(List.of(character));
        when(userCharacterRepository.findAllByUser(user)).thenReturn(List.of());

        List<ImageCharacterResponse> response = service.checkAndAwardCharacters(user);

        assertEquals(1, response.size());
        assertEquals("level-2", response.get(0).getName());
        verify(userCharacterRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void checkAndAwardCharacters_doesNotAwardWhenAttendanceIsMissing() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().name("user").email("user@example.com").build();
        ReflectionTestUtils.setField(user, "id", userId);
        ImageCharacter character = ImageCharacter.builder()
                .level(2)
                .name("level-2")
                .imageUrl("https://example.com/2.png")
                .isActive(true)
                .unlockTimeInSeconds(20 * 3600)
                .build();
        when(characterGrowthService.getStatus(userId))
                .thenReturn(new CharacterGrowthStatus(14, 100L * 3600));
        when(repository.findAllByOrderByLevelAsc()).thenReturn(List.of(character));
        when(userCharacterRepository.findAllByUser(user)).thenReturn(List.of());

        List<ImageCharacterResponse> response = service.checkAndAwardCharacters(user);

        assertEquals(0, response.size());
        verify(userCharacterRepository, org.mockito.Mockito.never())
                .saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void checkAndAwardCharacters_doesNotAwardWhenOnlyFocusTimeIsOneSecondShort() {
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ImageCharacter character = ImageCharacter.builder().level(2).isActive(true)
                .unlockTimeInSeconds(0).build();
        when(characterGrowthService.getStatus(user.getId()))
                .thenReturn(new CharacterGrowthStatus(100, 20 * 3_600L - 1));
        when(repository.findAllByOrderByLevelAsc()).thenReturn(List.of(character));
        when(userCharacterRepository.findAllByUser(user)).thenReturn(List.of());

        assertEquals(0, service.checkAndAwardCharacters(user).size());
        verify(userCharacterRepository, never()).saveAll(anyList());
    }

    @Test
    void checkAndAwardCharacters_doesNotDuplicateAnAlreadyOwnedCharacter() {
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ImageCharacter character = ImageCharacter.builder().level(2).isActive(true).build();
        ReflectionTestUtils.setField(character, "id", UUID.randomUUID());
        when(characterGrowthService.getStatus(user.getId()))
                .thenReturn(new CharacterGrowthStatus(15, 20 * 3_600L));
        when(repository.findAllByOrderByLevelAsc()).thenReturn(List.of(character));
        when(userCharacterRepository.findAllByUser(user))
                .thenReturn(List.of(UserCharacter.builder().user(user).imageCharacter(character).build()));

        assertEquals(0, service.checkAndAwardCharacters(user).size());
        verify(userCharacterRepository, never()).saveAll(anyList());
    }
}
