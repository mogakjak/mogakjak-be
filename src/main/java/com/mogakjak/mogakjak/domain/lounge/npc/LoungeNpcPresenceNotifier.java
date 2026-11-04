package com.mogakjak.mogakjak.domain.lounge.npc;

import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungeService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoungeNpcPresenceNotifier {
    private final OfficialLoungeService loungeService;

    @EventListener
    public void onUpdate(LoungeNpcUpdateEvent event) {
        loungeService.publishPresenceUpdate(event.loungeId(), null, "NPC_UPDATE");
    }
}
