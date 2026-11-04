package com.mogakjak.mogakjak.domain.lounge.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** 저장소나 실제 대기 없이 검증할 수 있는 공식 라운지 NPC 행동 규칙. */
public final class LoungeNpcPolicy {
    private final RandomGenerator random;

    public LoungeNpcPolicy(RandomGenerator random) {
        this.random = random;
    }

    public List<LoungeNpcState> reconcile(List<LoungeNpcState> previous, int realCount, int capacity, long now) {
        if (realCount < 0 || capacity < 0) {
            throw new IllegalArgumentException("인원과 정원은 음수일 수 없습니다.");
        }
        int target = Math.max(0, Math.min(3, capacity) - realCount);
        if (previous == null) {
            List<LoungeNpcState> initial = new ArrayList<>();
            for (LoungeNpcType type : LoungeNpcType.values()) {
                LoungeNpcState state = LoungeNpcState.absent(type);
                initial.add(type.ordinal() < target ? enter(state, now) : state);
            }
            return List.copyOf(initial);
        }

        List<LoungeNpcState> states = new ArrayList<>(previous);
        for (int i = 0; i < states.size(); i++) {
            LoungeNpcState state = states.get(i);
            if (state.present() && now >= state.activityEndsAt()) {
                states.set(i, state.leave());
            }
        }

        // 사용자 입장은 NPC 때문에 막지 않는다. 정원 초과만큼은 지연 없이 감축한다.
        int overCapacity = Math.max(0, presentCount(states) - Math.max(0, capacity - realCount));
        for (int i = states.size() - 1; i >= 0 && overCapacity > 0; i--) {
            if (states.get(i).present()) {
                states.set(i, states.get(i).leave());
                overCapacity--;
            }
        }

        int excess = Math.max(0, presentCount(states) - target);
        for (int i = states.size() - 1; i >= 0; i--) {
            LoungeNpcState state = states.get(i);
            if (!state.present()) {
                continue;
            }
            if (excess-- > 0) {
                long due = state.scheduledLeaveAt() != null ? state.scheduledLeaveAt() : now + delayMillis(180, 300);
                states.set(i, now >= due ? state.leave() : state.scheduleLeave(due));
            } else {
                states.set(i, state.scheduleLeave(null));
            }
        }

        int needed = Math.max(0, target - presentCount(states));
        for (int i = 0; i < states.size(); i++) {
            LoungeNpcState state = states.get(i);
            if (state.present()) {
                continue;
            }
            if (needed-- > 0) {
                long due = state.scheduledEnterAt() != null ? state.scheduledEnterAt() : now + delayMillis(0, 120);
                states.set(i, now >= due ? enter(state, now) : state.scheduleEnter(due));
            } else {
                states.set(i, state.scheduleEnter(null));
            }
        }
        return List.copyOf(states);
    }

    private LoungeNpcState enter(LoungeNpcState state, long now) {
        LoungeNpcType type = state.type();
        int taskIndex = state.taskIndex();
        if (taskIndex < 0) {
            taskIndex = random.nextInt(type.tasks().size());
        } else if (random.nextInt(100) < type.taskChangePercent()) {
            taskIndex = (taskIndex + 1 + random.nextInt(type.tasks().size() - 1)) % type.tasks().size();
        }
        return new LoungeNpcState(type, taskIndex, now,
                now + delayMillis(type.minActivitySeconds(), type.maxActivitySeconds()), null, null);
    }

    private long delayMillis(int minSeconds, int maxSeconds) {
        return random.nextInt(minSeconds, maxSeconds + 1) * 1000L;
    }

    private int presentCount(List<LoungeNpcState> states) {
        return (int) states.stream().filter(LoungeNpcState::present).count();
    }
}
