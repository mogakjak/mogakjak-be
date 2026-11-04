package com.mogakjak.mogakjak.domain.lounge.npc;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** 격리된 로컬 Redis에서 고유한 테스트 키만 사용한다. */
@EnabledIfEnvironmentVariable(named = "NPC_REDIS_TEST_PORT", matches = "[0-9]+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoungeNpcRedisTest {
    private LettuceConnectionFactory factory;
    private StringRedisTemplate redis;
    private LoungeNpcStore store;
    private String stateKey;
    private String presenceKey;
    private final LoungeNpcProperties properties = new LoungeNpcProperties();

    @BeforeAll
    void connect() {
        factory = new LettuceConnectionFactory("127.0.0.1", Integer.parseInt(System.getenv("NPC_REDIS_TEST_PORT")));
        factory.afterPropertiesSet();
        factory.start();
        redis = new StringRedisTemplate(factory);
        properties.setEnabled(true);
    }

    @BeforeEach
    void isolate() {
        String prefix = "npc-test:" + UUID.randomUUID();
        stateKey = prefix + ":state";
        presenceKey = prefix + ":presence";
        store = new LoungeNpcStore(redis, stateKey, presenceKey);
    }

    @AfterEach
    void clean() {
        redis.delete(List.of(stateKey, presenceKey));
    }

    @AfterAll
    void disconnect() {
        factory.destroy();
    }

    @Test
    void staleStateAndChangedRealPopulationBothRejectCas() {
        LoungeNpcStore.Stored empty = store.read();
        LoungeNpcService service = serviceAt(0);
        assertTrue(service.reconcile(10));
        LoungeNpcStore.Stored populated = store.read();
        assertFalse(store.compareAndSet(empty, 0, populated.snapshot()));
        addUsers(1);
        assertFalse(store.compareAndSet(populated, 0, new LoungeNpcSnapshot(2, List.of())));
        assertEquals(populated.snapshot(), store.read().snapshot());
    }

    @Test
    void simultaneousCasHasExactlyOneWinner() throws Exception {
        LoungeNpcStore.Stored empty = store.read();
        LoungeNpcSnapshot next = new LoungeNpcSnapshot(1, List.of(LoungeNpcState.absent(LoungeNpcType.A)));
        assertEquals(1L, concurrently(() -> store.compareAndSet(empty, 0, next)));
        assertEquals(next, store.read().snapshot());
    }

    @Test
    void concurrentInitializationAndDepartureHaveNoDuplicateTransitions() throws Exception {
        LoungeNpcService initial = serviceAt(0);
        assertEquals(1L, concurrently(() -> initial.reconcile(10)));
        assertEquals(3, initial.states().stream().filter(LoungeNpcState::present).count());
        assertEquals(1, store.read().snapshot().version());

        addUsers(3);
        LoungeNpcService reservation = serviceAt(1_000);
        assertEquals(1L, concurrently(() -> reservation.reconcile(10)));
        List<LoungeNpcState> reserved = reservation.states();
        assertTrue(reserved.stream().allMatch(s -> Long.valueOf(181_000).equals(s.scheduledLeaveAt())));
        LoungeNpcService due = serviceAt(181_000);
        assertEquals(1L, concurrently(() -> due.reconcile(10)));
        assertEquals(0, due.states().stream().filter(LoungeNpcState::present).count());
        assertEquals(3, store.read().snapshot().version());
        assertEquals(3, store.realMemberCount());
    }

    @Test
    void newServiceRestoresDeadlinesInsteadOfPostponingThem() {
        serviceAt(0).reconcile(10);
        addUsers(1);
        serviceAt(1_000).reconcile(10);
        LoungeNpcSnapshot reserved = store.read().snapshot();
        assertFalse(serviceAt(60_000).reconcile(10));
        assertEquals(reserved, store.read().snapshot());
        assertTrue(serviceAt(181_000).reconcile(10));
        assertFalse(serviceAt(181_000).states().get(2).present());
        assertTrue(serviceAt(181_000).states().get(0).present());
    }

    @Test
    void fullLoungeImmediatelyRemovesNpcWithoutChangingRealPresence() {
        serviceAt(0).reconcile(10);
        addUsers(10);
        serviceAt(1_000).reconcile(10);
        assertTrue(serviceAt(1_000).states().stream().noneMatch(LoungeNpcState::present));
        assertEquals(10, redis.opsForZSet().zCard(presenceKey));
    }

    @Test
    void cancelledReservationsAndMissingStateRecoverFromCurrentUsers() {
        serviceAt(0).reconcile(10);
        addUsers(1);
        serviceAt(1_000).reconcile(10);
        redis.delete(presenceKey);
        serviceAt(181_000).reconcile(10);
        assertTrue(serviceAt(181_000).states().stream().allMatch(LoungeNpcState::present));
        assertTrue(serviceAt(181_000).states().stream().allMatch(s -> s.scheduledLeaveAt() == null));

        redis.delete(stateKey);
        addUsers(2);
        assertTrue(serviceAt(200_000).reconcile(10));
        assertEquals(1, serviceAt(200_000).states().stream().filter(LoungeNpcState::present).count());
    }

    private LoungeNpcService serviceAt(long millis) {
        RandomGenerator random = mock(RandomGenerator.class);
        when(random.nextInt(anyInt(), anyInt())).thenAnswer(invocation -> invocation.getArgument(0));
        return new LoungeNpcService(properties, store,
                Clock.fixed(Instant.ofEpochMilli(millis), ZoneOffset.UTC), new LoungeNpcPolicy(random));
    }

    private void addUsers(int count) {
        for (int i = 0; i < count; i++) {
            redis.opsForZSet().add(presenceKey, UUID.randomUUID().toString(), i);
        }
    }

    private long concurrently(Callable<Boolean> operation) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                futures.add(pool.submit(() -> { start.await(); return operation.call(); }));
            }
            start.countDown();
            long winners = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(30, TimeUnit.SECONDS)) winners++;
            }
            return winners;
        } finally {
            pool.shutdownNow();
        }
    }
}
