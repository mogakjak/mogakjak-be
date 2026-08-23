package com.mogakjak.mogakjak.domain.user.service;

import com.mogakjak.mogakjak.domain.quote.repository.QuoteRepository;
import com.mogakjak.mogakjak.domain.timer.service.FocusTimeAggregationService;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.controller.dto.CharacterBasketResponse;
import com.mogakjak.mogakjak.domain.user.dto.response.TotalStudyTimeResponse;
import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserProfile;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserProfileRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyPageServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private TodoRepository todoRepository;
    @Mock private FocusTimeAggregationService focusTimeAggregationService;
    @Mock private ImageCharacterRepository imageCharacterRepository;
    @Mock private UserCharacterRepository userCharacterRepository;
    @Mock private QuoteRepository quoteRepository;

    @InjectMocks
    private MyPageServiceImpl service;

    @Test
    void characterBasketAndTotalStudyTime_useSameLifetimeFocusTime() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().name("user").email("user@example.com").build();
        ReflectionTestUtils.setField(user, "id", userId);
        UserProfile profile = UserProfile.builder().user(user).build();
        ImageCharacter defaultCharacter = ImageCharacter.builder()
                .level(1)
                .name("level-1")
                .imageUrl("https://example.com/1.png")
                .isActive(true)
                .unlockTimeInSeconds(0)
                .build();
        ReflectionTestUtils.setField(defaultCharacter, "id", UUID.randomUUID());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(todoRepository.countByUserAndIsCompletedAndIsDeletedFalse(user, true)).thenReturn(2L);
        when(focusTimeAggregationService.getLifetimeSeconds(userId)).thenReturn(7_260L);
        when(imageCharacterRepository.findAll()).thenReturn(List.of(defaultCharacter));
        when(userCharacterRepository.findAllByUser(user)).thenReturn(List.of());
        when(userCharacterRepository.findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(user))
                .thenReturn(Optional.empty());
        when(imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1))
                .thenReturn(Optional.of(defaultCharacter));

        CharacterBasketResponse basket = service.getCharacterBasket(userId);
        TotalStudyTimeResponse totalStudyTime = service.getTotalStudyTime(user);

        assertEquals("2시간 1분", basket.getTotalFocusTime());
        assertEquals(7_260L, totalStudyTime.getTotalStudyTime());
    }
}
