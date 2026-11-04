package com.mogakjak.mogakjak.domain.lounge.npc;

import java.util.List;

public record LoungeNpcSnapshot(long version, List<LoungeNpcState> states) {
    public LoungeNpcSnapshot {
        states = List.copyOf(states);
    }
}
