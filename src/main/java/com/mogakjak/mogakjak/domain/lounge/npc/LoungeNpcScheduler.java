package com.mogakjak.mogakjak.domain.lounge.npc;

import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lounge.npc", name = "enabled", havingValue = "true")
public class LoungeNpcScheduler {
    private final LoungeNpcService npcService;
    private final GroupRepository groupRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private long lastPublishedAt = Long.MIN_VALUE;

    @Scheduled(fixedDelay = 1000)
    public void tick() {
        try {
            groupRepository.findFirstByIsOfficialLoungeTrue().ifPresent(lounge -> {
                boolean changed = npcService.reconcile(lounge.getMaxMemberCount());
                long now = clock.millis();
                if (changed || lastPublishedAt == Long.MIN_VALUE || now - lastPublishedAt >= 10_000) {
                    events.publishEvent(new LoungeNpcUpdateEvent(lounge.getId()));
                    lastPublishedAt = now;
                }
            });
        } catch (Exception e) {
            log.error("공식 라운지 NPC 갱신 실패. 다음 tick에서 재시도합니다.", e);
        }
    }
}
