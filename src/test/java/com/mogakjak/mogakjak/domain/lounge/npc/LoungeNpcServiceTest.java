package com.mogakjak.mogakjak.domain.lounge.npc;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoungeNpcServiceTest {
    @Test
    void disabledByDefaultAndNeverTouchesRedis() {
        LoungeNpcStore store = mock(LoungeNpcStore.class);
        LoungeNpcService service = new LoungeNpcService(new LoungeNpcProperties(), store, Clock.systemUTC());
        assertFalse(service.enabled());
        assertFalse(service.reconcile(10));
        assertTrue(service.states().isEmpty());
        verifyNoInteractions(store);
    }

    @Test
    void retriesWithLatestPresenceAfterCasConflict() {
        LoungeNpcProperties properties = new LoungeNpcProperties();
        properties.setEnabled(true);
        LoungeNpcStore store = mock(LoungeNpcStore.class);
        when(store.read()).thenReturn(new LoungeNpcStore.Stored(null, null));
        when(store.realMemberCount()).thenReturn(0, 3);
        when(store.compareAndSet(any(), anyInt(), any())).thenReturn(false, true);
        LoungeNpcService service = new LoungeNpcService(properties, store, Clock.systemUTC());
        assertTrue(service.reconcile(10));
        verify(store).compareAndSet(any(), eq(0), argThat(s -> s.states().stream().allMatch(LoungeNpcState::present)));
        verify(store).compareAndSet(any(), eq(3), argThat(s -> s.states().stream().noneMatch(LoungeNpcState::present)));
    }

    @Test
    void repeatedConflictIsBoundedAndCanRetryOnNextTick() {
        LoungeNpcProperties properties = new LoungeNpcProperties();
        properties.setEnabled(true);
        LoungeNpcStore store = mock(LoungeNpcStore.class);
        when(store.read()).thenReturn(new LoungeNpcStore.Stored(null, null));
        LoungeNpcService service = new LoungeNpcService(properties, store, Clock.systemUTC());
        assertFalse(service.reconcile(10));
        verify(store, times(8)).compareAndSet(any(), eq(0), any());
    }

    @Test
    void unchangedStateIsNotWrittenAgain() {
        LoungeNpcProperties properties = new LoungeNpcProperties();
        properties.setEnabled(true);
        LoungeNpcStore store = mock(LoungeNpcStore.class);
        List<LoungeNpcState> empty = List.of(LoungeNpcState.absent(LoungeNpcType.A),
                LoungeNpcState.absent(LoungeNpcType.B), LoungeNpcState.absent(LoungeNpcType.C));
        when(store.read()).thenReturn(new LoungeNpcStore.Stored("stored", new LoungeNpcSnapshot(1, empty)));
        when(store.realMemberCount()).thenReturn(3);
        LoungeNpcService service = new LoungeNpcService(properties, store, Clock.systemUTC());
        assertFalse(service.reconcile(10));
        verify(store, never()).compareAndSet(any(), anyInt(), any());
    }
}
