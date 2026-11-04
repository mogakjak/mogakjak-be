package com.mogakjak.mogakjak.domain.lounge.npc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class LoungeNpcPolicyTest {
    private static final long NOW = 1_793_000_000_000L;
    private RandomGenerator random;
    private LoungeNpcPolicy policy;

    @BeforeEach
    void setUp() {
        random = mock(RandomGenerator.class);
        when(random.nextInt(anyInt(), anyInt())).thenAnswer(invocation -> invocation.getArgument(0));
        policy = new LoungeNpcPolicy(random);
    }

    @ParameterizedTest
    @CsvSource({"0,10,3", "1,10,2", "2,10,1", "3,10,0", "10,10,0", "0,2,2", "0,0,0"})
    void initialPopulation(int real, int capacity, int expected) {
        List<LoungeNpcState> states = policy.reconcile(null, real, capacity, NOW);
        assertEquals(expected, count(states));
        states.stream().filter(LoungeNpcState::present).forEach(state -> {
            assertEquals(NOW, state.enteredAt());
            assertEquals(NOW + state.type().minActivitySeconds() * 1000L, state.activityEndsAt());
        });
    }

    @Test
    void excessLeavesCThenBThenAWithoutPostponingExistingReservations() {
        List<LoungeNpcState> initial = policy.reconcile(null, 0, 10, NOW);
        List<LoungeNpcState> oneUser = policy.reconcile(initial, 1, 10, NOW);
        assertNull(oneUser.get(0).scheduledLeaveAt());
        assertNull(oneUser.get(1).scheduledLeaveAt());
        assertEquals(NOW + 180_000, oneUser.get(2).scheduledLeaveAt());

        List<LoungeNpcState> twoUsers = policy.reconcile(oneUser, 2, 10, NOW + 60_000);
        assertEquals(NOW + 240_000, twoUsers.get(1).scheduledLeaveAt());
        assertEquals(NOW + 180_000, twoUsers.get(2).scheduledLeaveAt());
        List<LoungeNpcState> afterC = policy.reconcile(twoUsers, 2, 10, NOW + 180_000);
        assertFalse(afterC.get(2).present());
        assertTrue(afterC.get(1).present());
        assertEquals(1, count(policy.reconcile(afterC, 2, 10, NOW + 240_000)));
    }

    @Test
    void userDepartureCancelsUnneededExitBeforeItsDeadline() {
        List<LoungeNpcState> initial = policy.reconcile(null, 0, 10, NOW);
        List<LoungeNpcState> pending = policy.reconcile(initial, 1, 10, NOW);
        List<LoungeNpcState> cancelled = policy.reconcile(pending, 0, 10, NOW + 180_000);
        assertEquals(3, count(cancelled));
        assertNull(cancelled.get(2).scheduledLeaveAt());
    }

    @Test
    void capacityTakesPrecedenceOverDelayedDeparture() {
        List<LoungeNpcState> initial = policy.reconcile(null, 0, 10, NOW);
        List<LoungeNpcState> almostFull = policy.reconcile(initial, 9, 10, NOW);
        assertEquals(1, count(almostFull));
        assertTrue(almostFull.getFirst().present());
        assertEquals(0, count(policy.reconcile(almostFull, 10, 10, NOW)));
    }

    @Test
    void refillIsDelayedAndRepeatedTicksDoNotReschedule() {
        when(random.nextInt(0, 121)).thenReturn(120);
        List<LoungeNpcState> empty = policy.reconcile(null, 3, 10, NOW);
        List<LoungeNpcState> pending = policy.reconcile(empty, 1, 10, NOW);
        assertEquals(NOW + 120_000, pending.get(0).scheduledEnterAt());
        assertEquals(NOW + 120_000, pending.get(1).scheduledEnterAt());
        assertNull(pending.get(2).scheduledEnterAt());
        assertEquals(pending, policy.reconcile(pending, 1, 10, NOW + 60_000));
        assertEquals(2, count(policy.reconcile(pending, 1, 10, NOW + 120_000)));
    }

    @Test
    void newUserCancelsUnneededRefillEvenWhenDue() {
        when(random.nextInt(0, 121)).thenReturn(120);
        List<LoungeNpcState> empty = policy.reconcile(null, 3, 10, NOW);
        List<LoungeNpcState> pending = policy.reconcile(empty, 2, 10, NOW);
        List<LoungeNpcState> cancelled = policy.reconcile(pending, 3, 10, NOW + 120_000);
        assertEquals(0, count(cancelled));
        assertNull(cancelled.getFirst().scheduledEnterAt());
    }

    @ParameterizedTest
    @EnumSource(LoungeNpcType.class)
    void expiryLeavesThenReentersAndResetsTimer(LoungeNpcType type) {
        when(random.nextInt(0, 121)).thenReturn(120);
        List<LoungeNpcState> states = List.of(LoungeNpcState.absent(LoungeNpcType.A),
                LoungeNpcState.absent(LoungeNpcType.B), LoungeNpcState.absent(LoungeNpcType.C));
        states = new java.util.ArrayList<>(states);
        states.set(type.ordinal(), new LoungeNpcState(type, 0, NOW - 50_000, NOW, null, null));
        List<LoungeNpcState> left = policy.reconcile(states, 0, 10, NOW);
        assertFalse(left.get(type.ordinal()).present());
        assertEquals(NOW + 120_000, left.get(type.ordinal()).scheduledEnterAt());
        LoungeNpcState reentered = policy.reconcile(left, 0, 10, NOW + 120_000).get(type.ordinal());
        assertEquals(NOW + 120_000, reentered.enteredAt());
        assertNotEquals(0, reentered.taskIndex());
    }

    @ParameterizedTest
    @EnumSource(LoungeNpcType.class)
    void taskChangeUsesStrictProbabilityBoundary(LoungeNpcType type) {
        List<LoungeNpcState> absent = new java.util.ArrayList<>(List.of(
                LoungeNpcState.absent(LoungeNpcType.A), LoungeNpcState.absent(LoungeNpcType.B),
                LoungeNpcState.absent(LoungeNpcType.C)));
        absent.set(type.ordinal(), new LoungeNpcState(type, 2, null, null, null, null));
        when(random.nextInt(100)).thenReturn(type.taskChangePercent());
        assertEquals(2, policy.reconcile(absent, 0, 10, NOW).get(type.ordinal()).taskIndex());
        when(random.nextInt(100)).thenReturn(type.taskChangePercent() - 1);
        assertNotEquals(2, policy.reconcile(absent, 0, 10, NOW).get(type.ordinal()).taskIndex());
    }

    @Test
    void randomEventsPreservePopulationAndActivityBounds() {
        Random events = new Random(123);
        LoungeNpcPolicy seeded = new LoungeNpcPolicy(new Random(456));
        List<LoungeNpcState> states = null;
        long now = NOW;
        for (int i = 0; i < 1000; i++) {
            int real = events.nextInt(11);
            states = seeded.reconcile(states, real, 10, now);
            assertTrue(count(states) <= 10 - real);
            for (LoungeNpcState state : states) {
                if (state.present()) {
                    long duration = state.activityEndsAt() - state.enteredAt();
                    assertTrue(duration >= state.type().minActivitySeconds() * 1000L);
                    assertTrue(duration <= state.type().maxActivitySeconds() * 1000L);
                    assertTrue(state.taskIndex() >= 0 && state.taskIndex() < state.type().tasks().size());
                }
            }
            now += events.nextInt(300_001);
        }
    }

    private long count(List<LoungeNpcState> states) {
        return states.stream().filter(LoungeNpcState::present).count();
    }
}
