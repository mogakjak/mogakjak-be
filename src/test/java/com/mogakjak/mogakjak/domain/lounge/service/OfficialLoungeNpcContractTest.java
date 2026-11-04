package com.mogakjak.mogakjak.domain.lounge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.lounge.dto.LoungeNpcMemberResponse;
import com.mogakjak.mogakjak.domain.lounge.npc.LoungeNpcType;
import com.mogakjak.mogakjak.domain.lounge.repository.OfficialLoungeAccessLogRepository;
import com.mogakjak.mogakjak.domain.quote.service.QuoteService;
import com.mogakjak.mogakjak.domain.timer.repository.*;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.*;
import com.mogakjak.mogakjak.global.websocket.service.CheerNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.RedisPubSubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfficialLoungeNpcContractTest {
    @Mock GroupRepository groups;
    @Mock UserRepository users;
    @Mock UserCharacterRepository characters;
    @Mock ImageCharacterRepository images;
    @Mock QuoteService quotes;
    @Mock OfficialLoungePresenceService presence;
    @Mock ActiveFocusSessionRepository active;
    @Mock FocusSessionRepository sessions;
    @Mock FocusIntervalRepository intervals;
    @Mock TodoRepository todos;
    @Mock OfficialLoungeAccessLogRepository accessLogs;
    @Mock CheerNotificationService cheers;
    @Mock RedisPubSubService pubsub;
    @Mock UserGroupRepository userGroups;
    @Mock LoungeNpcViewService npcs;
    @InjectMocks OfficialLoungeService lounge;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Group group = Group.builder().name("공식 라운지").build();
        group.markAsOfficialLounge(10);
        when(groups.findFirstByIsOfficialLoungeTrue()).thenReturn(Optional.of(group));
    }

    @Test
    void summaryKeepsRealUsersSeparateFromNpcAndDisplayCount() {
        when(npcs.members(0, 10)).thenReturn(List.of(npc(LoungeNpcType.A), npc(LoungeNpcType.B), npc(LoungeNpcType.C)));
        var summary = lounge.getSummary(userId);
        assertEquals(0L, summary.getCurrentMemberCount());
        assertEquals(3L, summary.getDisplayMemberCount());
        assertTrue(summary.getMembers().isEmpty());
        assertEquals(3, summary.getNpcMembers().size());
        verifyNoInteractions(sessions, active, intervals, todos, characters);
    }

    @Test
    void presenceBroadcastReconcilesAndPublishesBothKindsOfMembers() throws Exception {
        when(npcs.members(0, 10)).thenReturn(List.of(npc(LoungeNpcType.A)));
        lounge.publishPresenceUpdate(null, null, "NPC_UPDATE");
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(pubsub).publish(eq("official-lounge-presence"), body.capture());
        var json = new ObjectMapper().readTree(body.getValue());
        assertEquals(0, json.get("currentMemberCount").intValue());
        assertEquals(1, json.get("displayMemberCount").intValue());
        assertEquals(0, json.get("members").size());
        assertEquals("A", json.get("npcMembers").get(0).get("npcId").textValue());
        assertTrue(json.get("changedUserId").isNull());
        verify(npcs).reconcile(10);
    }

    @Test
    void entryUsesOnlyRealPresenceForAdmissionEvenWhenNpcAreDisplayed() {
        when(users.findById(userId)).thenReturn(Optional.of(User.builder().build()));
        when(presence.enter(userId, 10)).thenReturn(true);
        when(npcs.members(0, 10)).thenReturn(List.of(npc(LoungeNpcType.A), npc(LoungeNpcType.B), npc(LoungeNpcType.C)));
        assertDoesNotThrow(() -> lounge.enter(userId));
        verify(presence).enter(userId, 10);
        verify(npcs).reconcile(10);
        verifyNoInteractions(sessions, active, intervals, todos);
    }

    @Test
    void disabledNpcRetainsOriginalCounts() {
        var summary = lounge.getSummary(userId);
        assertEquals(0L, summary.getDisplayMemberCount());
        assertTrue(summary.getNpcMembers().isEmpty());
    }

    private LoungeNpcMemberResponse npc(LoungeNpcType type) {
        return new LoungeNpcMemberResponse(type.name(), type, type.nickname(), null, 1,
                GroupParticipationStatus.PARTICIPATING, LocalDateTime.of(2026, 11, 4, 18, 0), 30L,
                type.tasks().getFirst());
    }
}
