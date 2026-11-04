package com.mogakjak.mogakjak.domain.lounge.service;

import com.mogakjak.mogakjak.domain.lounge.dto.LoungeNpcMemberResponse;
import com.mogakjak.mogakjak.domain.lounge.npc.LoungeNpcService;
import com.mogakjak.mogakjak.domain.lounge.npc.LoungeNpcState;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoungeNpcViewService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final LoungeNpcService npcService;
    private final ImageCharacterRepository imageCharacterRepository;
    private final Clock clock;

    public void reconcile(int capacity) {
        try {
            npcService.reconcile(capacity);
        } catch (RuntimeException e) {
            log.warn("NPC 갱신에 실패했습니다. 실제 사용자 입퇴장은 유지하고 다음 tick에서 재시도합니다.", e);
        }
    }

    public List<LoungeNpcMemberResponse> members(int realCount, int capacity) {
        try {
            List<LoungeNpcState> present = npcService.states().stream()
                    .filter(LoungeNpcState::present)
                    .limit(Math.max(0, capacity - realCount))
                    .toList();
            if (present.isEmpty()) {
                return List.of();
            }
            String image = imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1)
                    .map(ImageCharacter::getImageUrl).orElse(null);
            long now = clock.millis();
            return present.stream().map(state -> new LoungeNpcMemberResponse(
                    state.type().name(), state.type(), state.type().nickname(), image, 1,
                    GroupParticipationStatus.PARTICIPATING,
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(state.enteredAt()), SEOUL),
                    Math.max(0, Math.min(now, state.activityEndsAt()) - state.enteredAt()) / 1000,
                    state.type().tasks().get(state.taskIndex())
            )).toList();
        } catch (RuntimeException e) {
            log.warn("NPC 표시를 읽지 못했습니다. 실제 사용자 현황만 반환합니다.", e);
            return List.of();
        }
    }
}
