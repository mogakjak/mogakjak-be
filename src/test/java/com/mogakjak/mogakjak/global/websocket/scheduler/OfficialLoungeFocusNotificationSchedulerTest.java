package com.mogakjak.mogakjak.global.websocket.scheduler;

import com.mogakjak.mogakjak.global.websocket.service.OfficialLoungeFocusNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OfficialLoungeFocusNotificationSchedulerTest {

    @Mock
    private OfficialLoungeFocusNotificationService officialLoungeFocusNotificationService;

    @InjectMocks
    private OfficialLoungeFocusNotificationScheduler scheduler;

    @Test
    void sendHourlyFocusNotification_delegatesToService() {
        scheduler.sendHourlyFocusNotification();

        verify(officialLoungeFocusNotificationService).sendHourlyFocusNotification();
    }
}
