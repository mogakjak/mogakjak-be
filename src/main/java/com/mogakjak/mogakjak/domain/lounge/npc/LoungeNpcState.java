package com.mogakjak.mogakjak.domain.lounge.npc;

/** Redis에 보관하는 NPC 상태. 시각은 시간대와 무관한 epoch milliseconds다. */
public record LoungeNpcState(
        LoungeNpcType type,
        int taskIndex,
        Long enteredAt,
        Long activityEndsAt,
        Long scheduledEnterAt,
        Long scheduledLeaveAt
) {
    public static LoungeNpcState absent(LoungeNpcType type) {
        return new LoungeNpcState(type, -1, null, null, null, null);
    }

    public boolean present() {
        return enteredAt != null;
    }

    public LoungeNpcState leave() {
        return new LoungeNpcState(type, taskIndex, null, null, null, null);
    }

    public LoungeNpcState scheduleEnter(Long at) {
        return new LoungeNpcState(type, taskIndex, enteredAt, activityEndsAt, at, scheduledLeaveAt);
    }

    public LoungeNpcState scheduleLeave(Long at) {
        return new LoungeNpcState(type, taskIndex, enteredAt, activityEndsAt, scheduledEnterAt, at);
    }
}
