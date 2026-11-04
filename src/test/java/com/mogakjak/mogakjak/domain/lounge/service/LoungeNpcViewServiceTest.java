package com.mogakjak.mogakjak.domain.lounge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mogakjak.mogakjak.domain.lounge.dto.LoungeNpcMemberResponse;
import com.mogakjak.mogakjak.domain.lounge.npc.*;
import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoungeNpcViewServiceTest {
    private final LoungeNpcService service = mock(LoungeNpcService.class);
    private final ImageCharacterRepository images = mock(ImageCharacterRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T09:00:30Z"), ZoneOffset.UTC);
    private final LoungeNpcViewService view = new LoungeNpcViewService(service, images, clock);

    @Test
    void emptyStateDoesNotReadImages() {
        assertTrue(view.members(0, 10).isEmpty());
        verifyNoInteractions(images);
    }

    @Test
    void rendersSeoulTimeElapsedSecondsAndExistingLevelOneImage() throws Exception {
        when(service.states()).thenReturn(List.of(state(LoungeNpcType.A), state(LoungeNpcType.B), state(LoungeNpcType.C)));
        when(images.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1))
                .thenReturn(Optional.of(ImageCharacter.builder().imageUrl("https://example.com/lv1.png").build()));
        List<LoungeNpcMemberResponse> members = view.members(1, 10);
        assertEquals(3, members.size()); // 감축 지연 중에는 목표 인원을 일시적으로 초과할 수 있다.
        LoungeNpcMemberResponse first = members.getFirst();
        assertEquals("A", first.npcId());
        assertEquals("모각이", first.nickname());
        assertEquals(1, first.level());
        assertEquals("https://example.com/lv1.png", first.profileUrl());
        assertEquals(LocalDateTime.of(2026, 11, 4, 18, 0), first.enteredAt());
        assertEquals(30L, first.personalTimerSeconds());
        assertEquals("코딩테스트 준비", first.todoTitle());
        var json = new ObjectMapper().registerModule(new JavaTimeModule()).valueToTree(first);
        assertFalse(json.has("userId"));
        assertFalse(json.has("todoId"));
        verify(images, times(1)).findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1);
    }

    @Test
    void limitsVisibleNpcByRealUserCapacity() {
        when(service.states()).thenReturn(List.of(state(LoungeNpcType.A), state(LoungeNpcType.B), state(LoungeNpcType.C)));
        assertEquals("A", view.members(9, 10).getFirst().npcId());
        assertTrue(view.members(10, 10).isEmpty());
    }

    @Test
    void missingImageFallsBackToNullAndElapsedTimeIsBounded() {
        long now = clock.millis();
        when(service.states()).thenReturn(List.of(
                new LoungeNpcState(LoungeNpcType.A, 0, now + 1000, now + 10_000, null, null),
                new LoungeNpcState(LoungeNpcType.B, 1, now - 20_000, now - 10_000, null, null)));
        var members = view.members(0, 10);
        assertNull(members.getFirst().profileUrl());
        assertEquals(0L, members.get(0).personalTimerSeconds());
        assertEquals(10L, members.get(1).personalTimerSeconds());
    }

    @Test
    void npcFailureDoesNotPreventRealUserResponseOrEntry() {
        when(service.states()).thenThrow(new IllegalStateException("redis unavailable"));
        doThrow(new IllegalStateException("redis unavailable")).when(service).reconcile(10);
        assertTrue(view.members(1, 10).isEmpty());
        assertDoesNotThrow(() -> view.reconcile(10));
    }

    private LoungeNpcState state(LoungeNpcType type) {
        long entered = clock.millis() - 30_000;
        return new LoungeNpcState(type, 0, entered, entered + type.minActivitySeconds() * 1000L, null, null);
    }
}
