package com.mogakjak.mogakjak.domain.group.service;

import com.mogakjak.mogakjak.domain.group.controller.dto.MyGroupResponse;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.invitation.repository.InvitationRepository;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeMemberResponse;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeSummaryResponse;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungeService;
import com.mogakjak.mogakjak.domain.user.entity.GroupRole;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.websocket.service.CheerNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.FocusNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.GroupMemberStatusService;
import com.mogakjak.mogakjak.global.websocket.service.GroupTimerService;
import com.mogakjak.mogakjak.global.websocket.service.InvitationNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.InvitationResponseNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.PokeNotificationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private FocusNotificationService focusNotificationService;

    @Mock
    private GroupMemberStatusService groupMemberStatusService;

    @Mock
    private PokeNotificationService pokeNotificationService;

    @Mock
    private CheerNotificationService cheerNotificationService;

    @Mock
    private InvitationNotificationService invitationNotificationService;

    @Mock
    private InvitationResponseNotificationService invitationResponseNotificationService;

    @Mock
    private OfficialLoungeService officialLoungeService;

    @Mock
    private UserCharacterRepository userCharacterRepository;

    @Mock
    private ImageCharacterRepository imageCharacterRepository;

    @Mock
    private GroupTimerService groupTimerService;

    @InjectMocks
    private GroupServiceImpl groupService;

    @Test
    void getMyGroups_prependsOfficialLoungeAndPreservesPrivateGroupShape() {
        UUID userId = UUID.randomUUID();
        UUID privateGroupId = UUID.randomUUID();
        UUID officialLoungeId = UUID.randomUUID();

        User user = User.builder()
                .name("kim")
                .email("kim@example.com")
                .imageUrl("https://img.example.com/me.png")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Group privateGroup = Group.builder()
                .name("private room")
                .imageUrl("https://img.example.com/private.png")
                .build();
        ReflectionTestUtils.setField(privateGroup, "id", privateGroupId);

        UserGroup userGroup = UserGroup.builder()
                .user(user)
                .group(privateGroup)
                .role(GroupRole.MEMBER)
                .build();

        User loungeMember = User.builder()
                .name("lounge friend")
                .email("friend@example.com")
                .imageUrl("https://img.example.com/lounge.png")
                .build();
        ReflectionTestUtils.setField(loungeMember, "id", UUID.randomUUID());

        OfficialLoungeSummaryResponse loungeSummary = OfficialLoungeSummaryResponse.builder()
                .loungeId(officialLoungeId)
                .loungeName("모각작 공식 라운지")
                .imageUrl("https://img.example.com/lounge-room.png")
                .currentMemberCount(1L)
                .maxMemberCount(20)
                .hasEntered(true)
                .myFocusCheckEnabled(true)
                .members(List.of(
                        OfficialLoungeMemberResponse.builder()
                                .userId(loungeMember.getId())
                                .nickname(loungeMember.getName())
                                .profileUrl(loungeMember.getImageUrl())
                                .level(1)
                                .build()
                ))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userGroupRepository.findAllByUserWithGroup(user)).thenReturn(List.of(userGroup));
        when(userGroupRepository.findByGroupIdWithUserAndProfile(privateGroupId)).thenReturn(List.of(userGroup));
        when(userCharacterRepository.findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(any()))
                .thenReturn(Optional.empty());
        when(officialLoungeService.getSummary(userId)).thenReturn(loungeSummary);

        List<MyGroupResponse> response = groupService.getMyGroups(userId);

        assertEquals(2, response.size());

        MyGroupResponse official = response.get(0);
        assertTrue(Boolean.TRUE.equals(official.getIsOfficialLounge()));
        assertEquals(officialLoungeId, official.getGroupId());
        assertEquals("모각작 공식 라운지", official.getGroupName());
        assertEquals(1L, official.getCurrentMemberCount());
        assertEquals(20, official.getMaxMemberCount());
        assertTrue(Boolean.TRUE.equals(official.getHasEntered()));
        assertTrue(Boolean.TRUE.equals(official.getMyFocusCheckEnabled()));
        assertNotNull(official.getMembers());
        assertEquals(1, official.getMembers().size());

        MyGroupResponse privateRoom = response.get(1);
        assertFalse(Boolean.TRUE.equals(privateRoom.getIsOfficialLounge()));
        assertEquals(privateGroupId, privateRoom.getGroupId());
        assertEquals("private room", privateRoom.getGroupName());
        assertNotNull(privateRoom.getMembers());
        assertEquals(1, privateRoom.getMembers().size());
        assertEquals(GroupRole.MEMBER, privateRoom.getMembers().get(0).getRole());
    }
}
