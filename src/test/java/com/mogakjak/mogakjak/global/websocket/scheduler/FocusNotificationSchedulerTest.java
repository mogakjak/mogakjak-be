package com.mogakjak.mogakjak.global.websocket.scheduler;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.global.websocket.service.FocusNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FocusNotificationSchedulerTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private FocusNotificationService focusNotificationService;

    @InjectMocks
    private FocusNotificationScheduler scheduler;

    @Test
    void sendFocusNotifications_sendsOnlyForEnabledGroup() {
        Group enabled = Group.builder().name("enabled").isNotificationAgreed(true).build();
        Group disabled = Group.builder().name("disabled").isNotificationAgreed(false).build();
        ReflectionTestUtils.setField(enabled, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(disabled, "id", UUID.randomUUID());
        when(groupRepository.findAll()).thenReturn(List.of(enabled, disabled));

        scheduler.sendFocusNotifications();

        verify(focusNotificationService).sendFocusNotificationToGroup(enabled.getId());
        verify(focusNotificationService, never()).sendFocusNotificationToGroup(disabled.getId());
        verify(groupRepository).save(enabled);
        verify(groupRepository, never()).save(disabled);
        assertNotNull(enabled.getLastNotificationSentAt());
    }
}
