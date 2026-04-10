package com.mogakjak.mogakjak.global.websocket.scheduler;

import com.mogakjak.mogakjak.global.websocket.service.OfficialLoungeFocusNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfficialLoungeFocusNotificationScheduler {

    private final OfficialLoungeFocusNotificationService officialLoungeFocusNotificationService;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void sendHourlyFocusNotification() {
        log.debug("공식 라운지 정각 집중 체크 스케줄러 실행");
        officialLoungeFocusNotificationService.sendHourlyFocusNotification();
    }
}
