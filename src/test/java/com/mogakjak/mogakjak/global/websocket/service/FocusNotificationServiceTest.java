package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.GroupRole;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.global.websocket.dto.FocusNotificationPublishDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FocusNotificationServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private RedisPubSubService redisPubSubService;

    @InjectMocks
    private FocusNotificationService service;

    @Test
    void sendFocusNotificationToGroup_includesRestingOptedInMemberWithoutActiveTimer() throws Exception {
        UUID groupId = UUID.randomUUID();
        Group group = group(groupId, true);
        UserGroup restingEnabled = membership(group, GroupParticipationStatus.RESTING, true, false);
        UserGroup participatingEnabled = membership(group, GroupParticipationStatus.PARTICIPATING, true, false);
        UserGroup restingDisabled = membership(group, GroupParticipationStatus.RESTING, false, false);
        UserGroup notParticipating = membership(group, GroupParticipationStatus.NOT_PARTICIPATING, true, false);
        UserGroup deleted = membership(group, GroupParticipationStatus.RESTING, true, true);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.findAllByGroupWithUser(group)).thenReturn(List.of(
                restingEnabled,
                participatingEnabled,
                restingDisabled,
                notParticipating,
                deleted
        ));

        service.sendFocusNotificationToGroup(groupId);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisPubSubService).publish(eq("focus-notification"), payloadCaptor.capture());
        FocusNotificationPublishDto payload = new ObjectMapper().readValue(
                payloadCaptor.getValue(),
                FocusNotificationPublishDto.class
        );

        assertEquals(
                Set.of(restingEnabled.getUser().getId(), participatingEnabled.getUser().getId()),
                Set.copyOf(payload.getRecipientUserIds())
        );
        assertEquals(groupId, payload.getNotification().getGroupId());
    }

    @Test
    void sendFocusNotificationToGroup_ignoresLegacyGroupToggleAndUsesPersonalOptIn() throws Exception {
        UUID groupId = UUID.randomUUID();
        Group group = group(groupId, false);
        UserGroup restingEnabled = membership(group, GroupParticipationStatus.RESTING, true, false);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.findAllByGroupWithUser(group)).thenReturn(List.of(restingEnabled));

        service.sendFocusNotificationToGroup(groupId);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisPubSubService).publish(eq("focus-notification"), payloadCaptor.capture());
        FocusNotificationPublishDto payload = new ObjectMapper().readValue(
                payloadCaptor.getValue(),
                FocusNotificationPublishDto.class
        );
        assertEquals(List.of(restingEnabled.getUser().getId()), payload.getRecipientUserIds());
    }

    private Group group(UUID groupId, boolean agreed) {
        Group group = Group.builder()
                .name("study")
                .isNotificationAgreed(agreed)
                .notificationMessage("집중 체크")
                .build();
        ReflectionTestUtils.setField(group, "id", groupId);
        return group;
    }

    private UserGroup membership(
            Group group,
            GroupParticipationStatus status,
            boolean enabled,
            boolean deleted
    ) {
        User user = User.builder()
                .name(UUID.randomUUID().toString())
                .email(UUID.randomUUID() + "@example.com")
                .isDeleted(deleted)
                .build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return UserGroup.builder()
                .user(user)
                .group(group)
                .role(GroupRole.MEMBER)
                .participationStatus(status)
                .isFocusCheckEnabled(enabled)
                .build();
    }
}
