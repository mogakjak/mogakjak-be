package com.mogakjak.mogakjak.global.websocket.scheduler;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.global.websocket.service.FocusNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FocusNotificationScheduler {

    private final GroupRepository groupRepository;
    private final FocusNotificationService focusNotificationService;

    /**
     * 1분마다 실행하여 각 그룹의 notificationCycle(시간 단위)에 맞춰 집중 체크 알림 전송
     */
    @Scheduled(fixedRate = 60000) // 1분 = 60000ms
    @Transactional
    public void sendFocusNotifications() {
        log.debug("집중 체크 알림 스케줄러 실행");
        
        List<Group> groups = groupRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Group group : groups) {
            // 공식 라운지는 별도 정각 스케줄러에서 처리
            if (Boolean.TRUE.equals(group.getIsOfficialLounge())) {
                continue;
            }

            // 마지막 알림 시간 확인
            LocalDateTime lastSentAt = group.getLastNotificationSentAt();
            if (lastSentAt != null) {
                // 마지막 알림 시간으로부터 notificationCycle(시간)이 지났는지 확인
                long hoursSinceLastNotification = java.time.Duration.between(lastSentAt, now).toHours();
                if (hoursSinceLastNotification < group.getNotificationCycle()) {
                    continue; // 아직 주기가 지나지 않음
                }
            }

            // 알림 전송
            try {
                boolean sent = focusNotificationService.sendFocusNotificationToGroup(group.getId());
                if (!sent) {
                    continue;
                }
                group.updateLastNotificationSentAt(now);
                groupRepository.save(group);
                log.debug("그룹 {}에 집중 체크 알림 전송 완료", group.getId());
            } catch (Exception e) {
                log.error("그룹 {}에 집중 체크 알림 전송 실패: {}", group.getId(), e.getMessage());
            }
        }
    }
}
