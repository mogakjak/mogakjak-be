package com.mogakjak.mogakjak.global.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.timer.entity.ActiveFocusSession;
import com.mogakjak.mogakjak.domain.timer.entity.FocusInterval;
import com.mogakjak.mogakjak.domain.timer.entity.FocusSession;
import com.mogakjak.mogakjak.domain.timer.enumerate.PomodoroPhaseType;
import com.mogakjak.mogakjak.domain.timer.enumerate.TimerStatus;
import com.mogakjak.mogakjak.domain.timer.repository.ActiveFocusSessionRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusIntervalRepository;
import com.mogakjak.mogakjak.domain.timer.repository.FocusSessionRepository;
import com.mogakjak.mogakjak.domain.todo.entity.Todo;
import com.mogakjak.mogakjak.domain.todo.repository.TodoRepository;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.GroupRole;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.global.websocket.dto.GroupMemberStatusDto;
import com.mogakjak.mogakjak.global.websocket.dto.GroupMemberStatusUpdateDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupMemberStatusServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private UserGroupRepository userGroupRepository;
    @Mock private ActiveFocusSessionRepository activeFocusSessionRepository;
    @Mock private FocusSessionRepository focusSessionRepository;
    @Mock private FocusIntervalRepository focusIntervalRepository;
    @Mock private TodoRepository todoRepository;
    @Mock private RedisPubSubService redisPubSubService;
    @Mock private UserCharacterRepository userCharacterRepository;
    @Mock private ImageCharacterRepository imageCharacterRepository;

    @InjectMocks
    private GroupMemberStatusService groupMemberStatusService;

    @Test
    void broadcastMemberStatusUpdate_includesLatestMemberCounts() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = org.mockito.Mockito.mock(User.class);
        Group group = org.mockito.Mockito.mock(Group.class);
        UserGroup userGroup = org.mockito.Mockito.mock(UserGroup.class);

        groupMemberStatusService.init();
        when(userGroupRepository.findByUser_IdAndGroup_Id(userId, groupId)).thenReturn(Optional.of(userGroup));
        when(userGroup.getUser()).thenReturn(user);
        when(userGroup.getGroup()).thenReturn(group);
        when(userGroup.getRole()).thenReturn(GroupRole.MEMBER);
        when(userGroup.getParticipationStatus()).thenReturn(GroupParticipationStatus.RESTING);
        when(group.getId()).thenReturn(groupId);
        when(user.getId()).thenReturn(userId);
        when(user.getName()).thenReturn("member");
        when(user.getIsDeleted()).thenReturn(false);
        when(activeFocusSessionRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userCharacterRepository.findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(user))
                .thenReturn(Optional.empty());
        when(imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1))
                .thenReturn(Optional.empty());
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.countActiveByGroup(group, GroupParticipationStatus.NOT_PARTICIPATING))
                .thenReturn(3L);
        when(userGroupRepository.countByGroup(group)).thenReturn(10L);

        groupMemberStatusService.broadcastMemberStatusUpdate(groupId, userId);

        org.mockito.ArgumentCaptor<String> payloadCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(redisPubSubService).publish(eq("group-member-status"), payloadCaptor.capture());
        GroupMemberStatusUpdateDto payload = new ObjectMapper().readValue(
                payloadCaptor.getValue(),
                GroupMemberStatusUpdateDto.class
        );
        assertEquals(3L, payload.getParticipatingMemberCount());
        assertEquals(10L, payload.getTotalMemberCount());
        assertEquals(userId, payload.getUpdatedMember().getUserId());
    }

    @Test
    void getMemberStatus_keepsExistingFieldAndReturnsTodoAccumulatedTimeInRealTime() {
        UUID groupId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID todoId = UUID.randomUUID();

        User user = org.mockito.Mockito.mock(User.class);
        Group group = org.mockito.Mockito.mock(Group.class);
        UserGroup userGroup = org.mockito.Mockito.mock(UserGroup.class);
        ActiveFocusSession activeSession = org.mockito.Mockito.mock(ActiveFocusSession.class);
        FocusSession focusSession = org.mockito.Mockito.mock(FocusSession.class);
        FocusInterval interval = org.mockito.Mockito.mock(FocusInterval.class);
        Todo todo = org.mockito.Mockito.mock(Todo.class);

        when(userGroupRepository.findByUser_IdAndGroup_Id(userId, groupId)).thenReturn(Optional.of(userGroup));
        when(userGroup.getUser()).thenReturn(user);
        when(userGroup.getGroup()).thenReturn(group);
        when(userGroup.getRole()).thenReturn(GroupRole.MEMBER);
        when(userGroup.getParticipationStatus()).thenReturn(GroupParticipationStatus.PARTICIPATING);
        when(group.getId()).thenReturn(groupId);
        when(user.getId()).thenReturn(userId);
        when(user.getName()).thenReturn("member");
        when(user.getIsDeleted()).thenReturn(false);

        when(activeFocusSessionRepository.findByUserId(userId)).thenReturn(Optional.of(activeSession));
        when(activeSession.getSessionId()).thenReturn(sessionId);
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(focusSession));
        when(focusSession.getId()).thenReturn(sessionId);
        when(focusSession.getTodoId()).thenReturn(todoId);
        when(focusSession.getStatus()).thenReturn(TimerStatus.RUNNING);
        when(focusSession.getIsTimerPublic()).thenReturn(true);
        when(focusSession.getIsTaskPublic()).thenReturn(false);
        when(todoRepository.findById(todoId)).thenReturn(Optional.of(todo));
        when(todo.getActualTimeInSeconds()).thenReturn(3_600);
        when(focusIntervalRepository.findTopBySessionIdOrderByStartedAtDesc(sessionId))
                .thenReturn(Optional.of(interval));
        when(interval.getPhaseType()).thenReturn(PomodoroPhaseType.NORMAL);
        when(interval.getStartedAt()).thenReturn(LocalDateTime.now().minusSeconds(15));
        when(userCharacterRepository.findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(user))
                .thenReturn(Optional.empty());
        when(imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1))
                .thenReturn(Optional.empty());

        GroupMemberStatusDto result = groupMemberStatusService.getMemberStatus(groupId, userId);

        assertTrue(result.getPersonalTimerSeconds() >= 3_614L);
        assertTrue(result.getPersonalTimerSeconds() <= 3_617L);
        assertNull(result.getTodoTitle());
    }
}
