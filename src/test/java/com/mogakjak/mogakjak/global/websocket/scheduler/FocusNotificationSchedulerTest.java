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
    void sendFocusNotifications_ignoresLegacyGroupToggleAndSkipsOfficialLounge() {
        Group enabled = Group.builder().name("enabled").isNotificationAgreed(true).build();
        Group disabled = Group.builder().name("disabled").isNotificationAgreed(false).build();
        Group officialLounge = Group.builder().name("official").build();
        officialLounge.markAsOfficialLounge(20);
        ReflectionTestUtils.setField(enabled, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(disabled, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(officialLounge, "id", UUID.randomUUID());
        when(groupRepository.findAll()).thenReturn(List.of(enabled, disabled, officialLounge));

        scheduler.sendFocusNotifications();

        verify(focusNotificationService).sendFocusNotificationToGroup(enabled.getId());
        verify(focusNotificationService).sendFocusNotificationToGroup(disabled.getId());
        verify(groupRepository).save(enabled);
        verify(groupRepository).save(disabled);
        verify(focusNotificationService, org.mockito.Mockito.never())
                .sendFocusNotificationToGroup(officialLounge.getId());
        verify(groupRepository, org.mockito.Mockito.never()).save(officialLounge);
        assertNotNull(enabled.getLastNotificationSentAt());
        assertNotNull(disabled.getLastNotificationSentAt());
    }
}
