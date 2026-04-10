package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungePresenceService;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationPublishDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficialLoungeFocusNotificationServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OfficialLoungePresenceService officialLoungePresenceService;

    @Mock
    private RedisPubSubService redisPubSubService;

    @InjectMocks
    private OfficialLoungeFocusNotificationService service;

    @Test
    void sendHourlyFocusNotification_publishesOnlyEnabledAndLiveUsers() throws Exception {
        UUID loungeId = UUID.randomUUID();
        UUID enabledUserId = UUID.randomUUID();
        UUID disabledUserId = UUID.randomUUID();
        UUID deletedUserId = UUID.randomUUID();

        Group lounge = Group.builder()
                .name("모각작 공식 라운지")
                .imageUrl("https://img.example.com/lounge.png")
                .notificationMessage("정각 집중 체크입니다.")
                .build();
        ReflectionTestUtils.setField(lounge, "id", loungeId);
        lounge.markAsOfficialLounge(20);

        User enabledUser = User.builder()
                .name("enabled")
                .email("enabled@example.com")
                .isDeleted(false)
                .isOfficialLoungeFocusCheckEnabled(true)
                .build();
        ReflectionTestUtils.setField(enabledUser, "id", enabledUserId);

        User disabledUser = User.builder()
                .name("disabled")
                .email("disabled@example.com")
                .isDeleted(false)
                .isOfficialLoungeFocusCheckEnabled(false)
                .build();
        ReflectionTestUtils.setField(disabledUser, "id", disabledUserId);

        User deletedUser = User.builder()
                .name("deleted")
                .email("deleted@example.com")
                .isDeleted(true)
                .isOfficialLoungeFocusCheckEnabled(true)
                .build();
        ReflectionTestUtils.setField(deletedUser, "id", deletedUserId);

        when(groupRepository.findFirstByIsOfficialLoungeTrue()).thenReturn(Optional.of(lounge));
        when(officialLoungePresenceService.findAllUserIds()).thenReturn(List.of(enabledUserId, disabledUserId, deletedUserId));
        when(userRepository.findAllById(any(Iterable.class))).thenReturn(List.of(enabledUser, disabledUser, deletedUser));

        service.sendHourlyFocusNotification();

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisPubSubService).publish(org.mockito.ArgumentMatchers.eq("focus-notification"), payloadCaptor.capture());
        FocusNotificationPublishDto publishDto = new ObjectMapper().readValue(payloadCaptor.getValue(), FocusNotificationPublishDto.class);

        assertNotNull(publishDto.getNotification());
        assertEquals(loungeId, publishDto.getNotification().getGroupId());
        assertEquals("모각작 공식 라운지", publishDto.getNotification().getGroupName());
        assertEquals("정각 집중 체크입니다.", publishDto.getNotification().getMessage());
        assertEquals(List.of(enabledUserId), publishDto.getRecipientUserIds());
        verify(officialLoungePresenceService).remove(deletedUserId);
    }

    @Test
    void sendHourlyFocusNotification_doesNothingWhenNoRecipients() {
        UUID loungeId = UUID.randomUUID();
        Group lounge = Group.builder()
                .name("모각작 공식 라운지")
                .imageUrl("https://img.example.com/lounge.png")
                .build();
        ReflectionTestUtils.setField(lounge, "id", loungeId);
        lounge.markAsOfficialLounge(20);

        when(groupRepository.findFirstByIsOfficialLoungeTrue()).thenReturn(Optional.of(lounge));
        when(officialLoungePresenceService.findAllUserIds()).thenReturn(List.of());

        service.sendHourlyFocusNotification();

        verifyNoInteractions(redisPubSubService);
    }
}
