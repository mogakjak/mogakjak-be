package com.mogakjak.mogakjak.domain.lounge.npc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.Random;

@Service
public class LoungeNpcService {
    private final LoungeNpcProperties properties;
    private final LoungeNpcStore store;
    private final Clock clock;
    private final LoungeNpcPolicy policy;

    @Autowired
    public LoungeNpcService(LoungeNpcProperties properties, LoungeNpcStore store, Clock clock) {
        this(properties, store, clock, new LoungeNpcPolicy(new Random()));
    }

    LoungeNpcService(LoungeNpcProperties properties, LoungeNpcStore store, Clock clock, LoungeNpcPolicy policy) {
        this.properties = properties;
        this.store = store;
        this.clock = clock;
        this.policy = policy;
    }

    public boolean enabled() {
        return properties.isEnabled();
    }

    public List<LoungeNpcState> states() {
        if (!enabled()) {
            return List.of();
        }
        LoungeNpcSnapshot snapshot = store.read().snapshot();
        return snapshot == null ? List.of() : snapshot.states();
    }

    public boolean reconcile(int capacity) {
        if (!enabled()) {
            return false;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            LoungeNpcStore.Stored stored = store.read();
            int realCount = store.realMemberCount();
            LoungeNpcSnapshot previous = stored.snapshot();
            List<LoungeNpcState> next = policy.reconcile(previous == null ? null : previous.states(),
                    realCount, capacity, clock.millis());
            if (previous != null && previous.states().equals(next)) {
                return false;
            }
            LoungeNpcSnapshot snapshot = new LoungeNpcSnapshot(previous == null ? 1 : previous.version() + 1, next);
            if (store.compareAndSet(stored, realCount, snapshot)) {
                return true;
            }
        }
        // 다음 tick에서 최신 인원을 다시 읽는다. 경쟁 때문에 사용자 입장을 실패시키지 않는다.
        return false;
    }
}
