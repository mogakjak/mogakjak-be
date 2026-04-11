package com.mogakjak.mogakjak.domain.lounge.service;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeMemberResponse;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeSummaryResponse;
import com.mogakjak.mogakjak.domain.quote.dto.QuoteResponse;
import com.mogakjak.mogakjak.domain.quote.service.QuoteService;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.domain.lounge.repository.OfficialLoungeAccessLogRepository;
import com.mogakjak.mogakjak.global.websocket.service.RedisPubSubService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OfficialLoungeServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCharacterRepository userCharacterRepository;

    @Mock
    private ImageCharacterRepository imageCharacterRepository;

    @Mock
    private QuoteService quoteService;

    @Mock
    private OfficialLoungePresenceService officialLoungePresenceService;

    @Mock
    private ActiveFocusSessionRepository activeFocusSessionRepository;

    @Mock
    private FocusSessionRepository focusSessionRepository;

    @Mock
    private FocusIntervalRepository focusIntervalRepository;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private OfficialLoungeAccessLogRepository officialLoungeAccessLogRepository;

    @Mock
    private RedisPubSubService redisPubSubService;

    @InjectMocks
    private OfficialLoungeService officialLoungeService;

    @Test
    void getSummary_returnsExpectedOfficialLoungePayload() {
        UUID loungeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Group lounge = Group.builder()
                .name("모각작 공식 라운지")
                .imageUrl("https://img.example.com/lounge-room.png")
                .build();
        ReflectionTestUtils.setField(lounge, "id", loungeId);
        lounge.markAsOfficialLounge(20);

        User user = User.builder()
                .name("kim")
                .email("kim@example.com")
                .imageUrl("https://img.example.com/me.png")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        QuoteResponse quote = QuoteResponse.builder()
                .id(UUID.randomUUID())
                .content("함께 몰입하면 더 멀리 간다")
                .author("모각작")
                .build();

        when(groupRepository.findFirstByIsOfficialLoungeTrue()).thenReturn(Optional.of(lounge));
        when(officialLoungePresenceService.findAllUserIds()).thenReturn(List.of(userId));
        when(officialLoungePresenceService.contains(userId)).thenReturn(true);
        when(userRepository.findAllById(any(Iterable.class))).thenReturn(List.of(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userCharacterRepository.findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(any()))
                .thenReturn(Optional.empty());
        when(activeFocusSessionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(focusSessionRepository.findTopByUserIdOrderByStartedAtDesc(userId)).thenReturn(Optional.empty());
        when(quoteService.getRandomQuote()).thenReturn(quote);

        OfficialLoungeSummaryResponse response = officialLoungeService.getSummary(userId);

        assertEquals(loungeId, response.getLoungeId());
        assertEquals("모각작 공식 라운지", response.getLoungeName());
        assertEquals("https://img.example.com/lounge-room.png", response.getImageUrl());
        assertEquals(1L, response.getCurrentMemberCount());
        assertEquals(20, response.getMaxMemberCount());
        assertTrue(Boolean.TRUE.equals(response.getHasEntered()));
        assertTrue(Boolean.TRUE.equals(response.getMyFocusCheckEnabled()));
        assertNotNull(response.getTodayQuote());
        assertEquals("함께 몰입하면 더 멀리 간다", response.getTodayQuote().getContent());
        assertEquals(1, response.getMembers().size());
        OfficialLoungeMemberResponse memberResponse = response.getMembers().get(0);
        assertEquals(userId, memberResponse.getUserId());
        assertEquals("kim", memberResponse.getNickname());
        assertEquals("https://img.example.com/me.png", memberResponse.getProfileUrl());
        assertEquals(1, memberResponse.getLevel());
        assertEquals("NOT_PARTICIPATING", memberResponse.getParticipationStatus());
    }

    @Test
    void enter_publishesOfficialLoungePresenceUpdate() {
        UUID loungeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Group lounge = Group.builder()
                .name("모각작 공식 라운지")
                .imageUrl("https://img.example.com/lounge-room.png")
                .build();
        ReflectionTestUtils.setField(lounge, "id", loungeId);
        lounge.markAsOfficialLounge(20);

        User user = User.builder()
                .name("kim")
                .email("kim@example.com")
                .imageUrl("https://img.example.com/me.png")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        QuoteResponse quote = QuoteResponse.builder()
                .id(UUID.randomUUID())
                .content("함께 몰입하면 더 멀리 간다")
                .author("모각작")
                .build();

        when(groupRepository.findFirstByIsOfficialLoungeTrue()).thenReturn(Optional.of(lounge));
        when(officialLoungePresenceService.enter(userId, 20)).thenReturn(true);
        when(officialLoungePresenceService.findAllUserIds()).thenReturn(List.of(userId));
        when(officialLoungePresenceService.contains(userId)).thenReturn(true);
        when(userRepository.findAllById(any(Iterable.class))).thenReturn(List.of(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userCharacterRepository.findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(any()))
                .thenReturn(Optional.empty());
        when(activeFocusSessionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(focusSessionRepository.findTopByUserIdOrderByStartedAtDesc(userId)).thenReturn(Optional.empty());
        when(quoteService.getRandomQuote()).thenReturn(quote);

        officialLoungeService.enter(userId);

        verify(redisPubSubService).publish(eq("official-lounge-presence"), anyString());
    }
}
