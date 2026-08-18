package com.mogakjak.mogakjak.domain.lounge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeMemberResponse;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeSummaryResponse;
import com.mogakjak.mogakjak.domain.quote.dto.QuoteResponse;
import com.mogakjak.mogakjak.domain.quote.service.QuoteService;
import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.domain.lounge.repository.OfficialLoungeAccessLogRepository;
import com.mogakjak.mogakjak.global.websocket.service.RealtimeEventPublisher;
import com.mogakjak.mogakjak.global.websocket.service.CheerNotificationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

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
    private RealtimeEventPublisher realtimeEventPublisher;

    @Mock
    private CheerNotificationService cheerNotificationService;

    @Mock
    private UserGroupRepository userGroupRepository;

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
        when(officialLoungePresenceService.getEnteredAtMap(List.of(userId)))
                .thenReturn(Map.of(userId, LocalDateTime.now().minusHours(1)));
        when(officialLoungePresenceService.getCheerCountMap(List.of(userId))).thenReturn(Map.of(userId, 3));
        when(userRepository.findAllById(any(Iterable.class))).thenReturn(List.of(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userCharacterRepository.findAllByUserIdInOrderByUserIdAscImageCharacter_LevelDescImageCharacter_CreatedAtAsc(anyCollection()))
                .thenReturn(List.of());
        when(activeFocusSessionRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());
        when(focusSessionRepository.findAllByUserIdInOrderByUserIdAscStartedAtDesc(anyCollection())).thenReturn(List.of());
        when(focusIntervalRepository.findAllBySessionIdInOrderBySessionIdAscStartedAtDesc(anyCollection())).thenReturn(List.of());
        when(imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1))
                .thenReturn(Optional.of(ImageCharacter.builder()
                        .level(1)
                        .name("default")
                        .imageUrl("https://img.example.com/default.png")
                        .isActive(true)
                        .unlockTimeInSeconds(0)
                        .build()));
        when(quoteService.getRandomQuote()).thenReturn(quote);
        when(userGroupRepository.findMateIdsByUser(any(UUID.class), anyCollection())).thenReturn(Set.of());

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
        assertNotNull(memberResponse.getEnteredAt());
        assertEquals(3, memberResponse.getCheerCount());
    }

    @Test
    void sendCheer_updatesTargetCheerCount_andPublishesPresence() {
        UUID loungeId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        Group lounge = Group.builder()
                .name("모각작 공식 라운지")
                .imageUrl("https://img.example.com/lounge-room.png")
                .build();
        ReflectionTestUtils.setField(lounge, "id", loungeId);
        lounge.markAsOfficialLounge(20);

        User sender = User.builder()
                .name("sender")
                .email("sender@example.com")
                .imageUrl("https://img.example.com/sender.png")
                .build();
        ReflectionTestUtils.setField(sender, "id", senderId);

        User target = User.builder()
                .name("target")
                .email("target@example.com")
                .imageUrl("https://img.example.com/target.png")
                .build();
        ReflectionTestUtils.setField(target, "id", targetId);

        when(groupRepository.findFirstByIsOfficialLoungeTrue()).thenReturn(Optional.of(lounge));
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(officialLoungePresenceService.contains(senderId)).thenReturn(true);
        when(officialLoungePresenceService.contains(targetId)).thenReturn(true);
        when(officialLoungePresenceService.incrementCheerCount(targetId)).thenReturn(1L);
        when(officialLoungePresenceService.findAllUserIds()).thenReturn(List.of(senderId, targetId));
        when(userRepository.findAllById(any(Iterable.class))).thenReturn(List.of(sender, target));
        when(userCharacterRepository.findAllByUserIdInOrderByUserIdAscImageCharacter_LevelDescImageCharacter_CreatedAtAsc(anyCollection()))
                .thenReturn(List.of());
        when(activeFocusSessionRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());
        when(focusSessionRepository.findAllByUserIdInOrderByUserIdAscStartedAtDesc(anyCollection())).thenReturn(List.of());
        when(focusIntervalRepository.findAllBySessionIdInOrderBySessionIdAscStartedAtDesc(anyCollection())).thenReturn(List.of());
        when(officialLoungePresenceService.getEnteredAtMap(List.of(senderId, targetId)))
                .thenReturn(Map.of(
                        senderId, LocalDateTime.now().minusMinutes(5),
                        targetId, LocalDateTime.now().minusMinutes(10)
                ));
        when(officialLoungePresenceService.getCheerCountMap(List.of(senderId, targetId)))
                .thenReturn(Map.of(senderId, 0, targetId, 1));
        when(imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1))
                .thenReturn(Optional.of(ImageCharacter.builder()
                        .level(1)
                        .name("default")
                        .imageUrl("https://img.example.com/default.png")
                        .isActive(true)
                        .unlockTimeInSeconds(0)
                        .build()));

        officialLoungeService.sendCheer(senderId, targetId);

        verify(officialLoungePresenceService).incrementCheerCount(targetId);
        verify(cheerNotificationService).sendCheerNotification(senderId, targetId, loungeId);
        verify(realtimeEventPublisher).publish(eq("official-lounge-presence"), anyString());
    }

    @Test
    void enter_withDeltaPayload_publishesOnlyChangedMember() throws Exception {
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
        when(officialLoungePresenceService.count()).thenReturn(1L);
        when(officialLoungePresenceService.findAllUserIds()).thenReturn(List.of(userId));
        when(officialLoungePresenceService.contains(userId)).thenReturn(true);
        when(userRepository.findAllById(any(Iterable.class))).thenReturn(List.of(user));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userCharacterRepository.findAllByUserIdInOrderByUserIdAscImageCharacter_LevelDescImageCharacter_CreatedAtAsc(anyCollection()))
                .thenReturn(List.of());
        when(activeFocusSessionRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of());
        when(focusSessionRepository.findAllByUserIdInOrderByUserIdAscStartedAtDesc(anyCollection())).thenReturn(List.of());
        when(focusIntervalRepository.findAllBySessionIdInOrderBySessionIdAscStartedAtDesc(anyCollection())).thenReturn(List.of());
        when(officialLoungePresenceService.getEnteredAtMap(List.of(userId)))
                .thenReturn(Map.of(userId, LocalDateTime.now().minusMinutes(1)));
        when(officialLoungePresenceService.getCheerCountMap(List.of(userId))).thenReturn(Map.of(userId, 0));
        when(imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1))
                .thenReturn(Optional.of(ImageCharacter.builder()
                        .level(1)
                        .name("default")
                        .imageUrl("https://img.example.com/default.png")
                        .isActive(true)
                        .unlockTimeInSeconds(0)
                        .build()));
        when(quoteService.getRandomQuote()).thenReturn(quote);
        when(userGroupRepository.findMateIdsByUser(any(UUID.class), anyCollection())).thenReturn(Set.of());
        ReflectionTestUtils.setField(officialLoungeService, "loungePayloadMode", "delta");

        officialLoungeService.enter(userId);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(realtimeEventPublisher).publish(eq("official-lounge-presence"), payloadCaptor.capture());
        JsonNode payload = new ObjectMapper().readTree(payloadCaptor.getValue());
        assertFalse(payload.has("members"));
        assertEquals(userId.toString(), payload.path("changedMember").path("userId").asText());
        assertEquals(1L, payload.path("currentMemberCount").asLong());
    }

    @Test
    void getSummary_returnsTodoAccumulatedTimeForOfficialLoungeMember() {
        UUID loungeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();

        Group lounge = Group.builder().name("모각작 공식 라운지").build();
        ReflectionTestUtils.setField(lounge, "id", loungeId);
        lounge.markAsOfficialLounge(20);

        User user = User.builder().name("member").email("member@example.com").build();
        ReflectionTestUtils.setField(user, "id", userId);

        ActiveFocusSession activeSession = mock(ActiveFocusSession.class);
        FocusSession focusSession = mock(FocusSession.class);
        FocusInterval interval = mock(FocusInterval.class);
        Todo todo = mock(Todo.class);

        when(groupRepository.findFirstByIsOfficialLoungeTrue()).thenReturn(Optional.of(lounge));
        when(officialLoungePresenceService.findAllUserIds()).thenReturn(List.of(userId));
        when(officialLoungePresenceService.getEnteredAtMap(List.of(userId)))
                .thenReturn(Map.of(userId, LocalDateTime.now().minusMinutes(1)));
        when(officialLoungePresenceService.getCheerCountMap(List.of(userId))).thenReturn(Map.of(userId, 0));
        when(userRepository.findAllById(any(Iterable.class))).thenReturn(List.of(user));
        when(userCharacterRepository.findAllByUserIdInOrderByUserIdAscImageCharacter_LevelDescImageCharacter_CreatedAtAsc(anyCollection()))
                .thenReturn(List.of());
        when(imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1))
                .thenReturn(Optional.empty());
        when(userGroupRepository.findMateIdsByUser(any(UUID.class), anyCollection())).thenReturn(Set.of());

        when(activeSession.getUserId()).thenReturn(userId);
        when(activeSession.getSessionId()).thenReturn(sessionId);
        when(activeFocusSessionRepository.findAllByUserIdIn(anyCollection())).thenReturn(List.of(activeSession));
        when(focusSession.getUserId()).thenReturn(userId);
        when(focusSession.getId()).thenReturn(sessionId);
        when(focusSession.getTodoId()).thenReturn(todoId);
        when(focusSession.getStatus()).thenReturn(TimerStatus.RUNNING);
        when(focusSession.getIsTimerPublic()).thenReturn(true);
        when(focusSession.getIsTaskPublic()).thenReturn(true);
        when(focusSession.getStartedAt()).thenReturn(LocalDateTime.now().minusMinutes(1));
        when(focusSessionRepository.findAllByUserIdInOrderByUserIdAscStartedAtDesc(anyCollection()))
                .thenReturn(List.of(focusSession));
        when(interval.getSessionId()).thenReturn(sessionId);
        when(interval.getPhaseType()).thenReturn(PomodoroPhaseType.NORMAL);
        when(interval.getStartedAt()).thenReturn(LocalDateTime.now().minusSeconds(15));
        when(focusIntervalRepository.findAllBySessionIdInOrderBySessionIdAscStartedAtDesc(anyCollection()))
                .thenReturn(List.of(interval));
        when(todo.getId()).thenReturn(todoId);
        when(todo.getActualTimeInSeconds()).thenReturn(7_200);
        when(todo.getTask()).thenReturn("누적할 작업");
        when(todoRepository.findAllById(any(Iterable.class))).thenReturn(List.of(todo));

        OfficialLoungeSummaryResponse response = officialLoungeService.getSummary(null);

        OfficialLoungeMemberResponse member = response.getMembers().get(0);
        assertTrue(member.getPersonalTimerSeconds() >= 7_214L);
        assertTrue(member.getPersonalTimerSeconds() <= 7_217L);
        assertEquals("누적할 작업", member.getTodoTitle());
    }
}
