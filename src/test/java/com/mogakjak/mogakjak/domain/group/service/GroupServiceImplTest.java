package com.mogakjak.mogakjak.domain.group.service;

import com.mogakjak.mogakjak.domain.group.controller.dto.MyGroupResponse;
import com.mogakjak.mogakjak.domain.group.controller.dto.FocusNotificationRequest;
import com.mogakjak.mogakjak.domain.group.controller.dto.GroupFocusCheckRequest;
import com.mogakjak.mogakjak.domain.group.controller.dto.GroupFocusCheckResponse;
import com.mogakjak.mogakjak.domain.group.controller.dto.InviteMateResponse;
import com.mogakjak.mogakjak.domain.group.controller.dto.InviteMateStatus;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.invitation.repository.InvitationRepository;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeMemberResponse;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeSummaryResponse;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungeService;
import com.mogakjak.mogakjak.domain.user.entity.GroupRole;
import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserCharacter;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.domain.user.repository.projection.SharedGroupNameProjection;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.mogakjak.mogakjak.domain.invitation.controller.dto.InviteMateRequest;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.domain.invitation.entity.Invitation;
import com.mogakjak.mogakjak.domain.invitation.entity.InvitationStatus;

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
                                .isMate(true)
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
        assertTrue(Boolean.TRUE.equals(official.getMembers().get(0).getIsMate()));

        MyGroupResponse privateRoom = response.get(1);
        assertFalse(Boolean.TRUE.equals(privateRoom.getIsOfficialLounge()));
        assertEquals(privateGroupId, privateRoom.getGroupId());
        assertEquals("private room", privateRoom.getGroupName());
        assertNotNull(privateRoom.getMembers());
        assertEquals(1, privateRoom.getMembers().size());
        assertEquals(GroupRole.MEMBER, privateRoom.getMembers().get(0).getRole());
    }

    @Test
    void updateMyFocusCheck_updatesOnlyCurrentMembershipSetting() {
        UUID groupId = UUID.randomUUID();
        User user = User.builder().name("member").email("member@example.com").build();
        Group group = Group.builder().name("study").build();
        ReflectionTestUtils.setField(group, "id", groupId);
        UserGroup userGroup = UserGroup.create(user, group, GroupRole.MEMBER);
        assertTrue(Boolean.TRUE.equals(userGroup.getIsFocusCheckEnabled()));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group)).thenReturn(Optional.of(userGroup));

        GroupFocusCheckResponse response = groupService.updateMyFocusCheck(
                user,
                groupId,
                new GroupFocusCheckRequest(false)
        );

        assertFalse(Boolean.TRUE.equals(response.myFocusCheckEnabled()));
        assertFalse(Boolean.TRUE.equals(userGroup.getIsFocusCheckEnabled()));
        assertTrue(group.getIsNotificationAgreed());
    }

    @Test
    void modifyFocusNotification_rejectsNonHostMember() {
        UUID groupId = UUID.randomUUID();
        User user = User.builder().name("member").email("member@example.com").build();
        Group group = Group.builder().name("study").build();
        ReflectionTestUtils.setField(group, "id", groupId);
        UserGroup userGroup = UserGroup.create(user, group, GroupRole.MEMBER);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group)).thenReturn(Optional.of(userGroup));

        com.mogakjak.mogakjak.global.exception.CustomException exception = assertThrows(
                com.mogakjak.mogakjak.global.exception.CustomException.class,
                () -> groupService.modifyFocusNotification(
                        user,
                        groupId,
                        new FocusNotificationRequest(true, 2, "집중")
                )
        );

        assertEquals(ErrorCode.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void getInviteMates_marksInviteStatusPerMate() {
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID mateOneId = UUID.randomUUID();
        UUID mateTwoId = UUID.randomUUID();

        User user = User.builder()
                .name("host")
                .email("host@example.com")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Group group = Group.builder()
                .name("study")
                .imageUrl("https://img.example.com/group.png")
                .build();
        ReflectionTestUtils.setField(group, "id", groupId);

        User mateOne = User.builder()
                .name("mate-one")
                .email("mate1@example.com")
                .imageUrl(null)
                .build();
        ReflectionTestUtils.setField(mateOne, "id", mateOneId);

        User mateTwo = User.builder()
                .name("mate-two")
                .email("mate2@example.com")
                .imageUrl("https://img.example.com/mate-two.png")
                .build();
        ReflectionTestUtils.setField(mateTwo, "id", mateTwoId);

        Page<User> matePage = new PageImpl<>(List.of(mateOne, mateTwo), PageRequest.of(0, 10), 2);
        UserGroup hostUserGroup = UserGroup.builder()
                .user(user)
                .group(group)
                .role(GroupRole.HOST)
                .build();
        UserCharacter mateOneCharacter = UserCharacter.builder()
                .user(mateOne)
                .imageCharacter(ImageCharacter.builder()
                        .level(3)
                        .name("leaf")
                        .imageUrl("https://img.example.com/fallback.png")
                        .isActive(true)
                        .unlockTimeInSeconds(0)
                        .build())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group)).thenReturn(Optional.of(hostUserGroup));
        when(userGroupRepository.findTotalMatesByUser(user, null, PageRequest.of(0, 10))).thenReturn(matePage);
        when(userGroupRepository.findAllByGroupWithUser(group)).thenReturn(List.of(hostUserGroup));
        when(userGroupRepository.findSharedGroupNamesByMates(user, List.of(mateOneId, mateTwoId))).thenReturn(List.of(
                sharedGroupName(mateOneId, "shared-a"),
                sharedGroupName(mateTwoId, "shared-a"),
                sharedGroupName(mateTwoId, "shared-b")
        ));
        when(userCharacterRepository.findAllByUserIdInOrderByUserIdAscImageCharacter_LevelDescImageCharacter_CreatedAtAsc(List.of(mateOneId, mateTwoId)))
                .thenReturn(List.of(mateOneCharacter));
        when(imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1))
                .thenReturn(Optional.empty());
        when(invitationRepository.findInviteeIdsByGroupAndStatusAndInviteeIds(group, List.of(mateOneId, mateTwoId), com.mogakjak.mogakjak.domain.invitation.entity.InvitationStatus.PENDING))
                .thenReturn(List.of(mateOneId));

        Page<InviteMateResponse> response = groupService.getInviteMates(userId, groupId, null, PageRequest.of(0, 10));

        assertEquals(2, response.getTotalElements());
        assertEquals(InviteMateStatus.ALREADY_INVITED, response.getContent().get(0).getInviteStatus());
        assertEquals(InviteMateStatus.CAN_INVITE, response.getContent().get(1).getInviteStatus());
        assertEquals(List.of("shared-a"), response.getContent().get(0).getGroupNames());
        assertEquals("https://img.example.com/fallback.png", response.getContent().get(0).getProfileUrl());
        assertEquals(Integer.valueOf(3), response.getContent().get(0).getLevel());
        assertEquals("https://img.example.com/mate-two.png", response.getContent().get(1).getProfileUrl());
    }

    @Test
    void getInviteMates_requiresGroupMembership() {
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        User user = User.builder()
                .name("user")
                .email("user@example.com")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Group group = Group.builder()
                .name("study")
                .build();
        ReflectionTestUtils.setField(group, "id", groupId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(user, group)).thenReturn(Optional.empty());

        com.mogakjak.mogakjak.global.exception.CustomException ex = assertThrows(
                com.mogakjak.mogakjak.global.exception.CustomException.class,
                () -> groupService.getInviteMates(userId, groupId, null, PageRequest.of(0, 10))
        );

        assertEquals(ErrorCode.ONLY_GROUP_MEMBER_CAN_VIEW_INVITE_MATES, ex.getStatusCode());
        assertEquals("그룹 멤버만 초대 가능한 메이트를 조회할 수 있습니다.", ex.getMessage());
    }

    @Test
    void getInviteMates_allowsOfficialLoungeWhenEntered() {
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID mateId = UUID.randomUUID();

        User user = User.builder()
                .name("user")
                .email("user@example.com")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Group lounge = Group.builder()
                .name("모각작 공식 라운지")
                .build();
        ReflectionTestUtils.setField(lounge, "id", groupId);
        ReflectionTestUtils.setField(lounge, "isOfficialLounge", true);

        User mate = User.builder()
                .name("mate")
                .email("mate@example.com")
                .build();
        ReflectionTestUtils.setField(mate, "id", mateId);

        Page<User> matePage = new PageImpl<>(List.of(mate), PageRequest.of(0, 10), 1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(lounge));
        when(officialLoungeService.isEntered(userId)).thenReturn(true);
        when(officialLoungeService.isEntered(mateId)).thenReturn(true);
        when(userGroupRepository.findTotalMatesByUser(user, null, PageRequest.of(0, 10))).thenReturn(matePage);
        when(userGroupRepository.findAllByGroupWithUser(lounge)).thenReturn(List.of());
        when(userGroupRepository.findSharedGroupNamesByMates(user, List.of(mateId))).thenReturn(List.of());
        when(userCharacterRepository.findAllByUserIdInOrderByUserIdAscImageCharacter_LevelDescImageCharacter_CreatedAtAsc(List.of(mateId)))
                .thenReturn(List.of());
        when(imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1))
                .thenReturn(Optional.empty());
        when(invitationRepository.findInviteeIdsByGroupAndStatusAndInviteeIds(lounge, List.of(mateId), com.mogakjak.mogakjak.domain.invitation.entity.InvitationStatus.PENDING))
                .thenReturn(List.of());

        Page<InviteMateResponse> response = groupService.getInviteMates(userId, groupId, null, PageRequest.of(0, 10));

        assertEquals(1, response.getTotalElements());
        assertEquals(InviteMateStatus.ALREADY_IN_OFFICIAL_LOUNGE, response.getContent().get(0).getInviteStatus());
    }

    @Test
    void getInviteMates_requiresOfficialLoungeEntry() {
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        User user = User.builder()
                .name("user")
                .email("user@example.com")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Group lounge = Group.builder()
                .name("모각작 공식 라운지")
                .build();
        ReflectionTestUtils.setField(lounge, "id", groupId);
        ReflectionTestUtils.setField(lounge, "isOfficialLounge", true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(lounge));
        when(officialLoungeService.isEntered(userId)).thenReturn(false);

        com.mogakjak.mogakjak.global.exception.CustomException ex = assertThrows(
                com.mogakjak.mogakjak.global.exception.CustomException.class,
                () -> groupService.getInviteMates(userId, groupId, null, PageRequest.of(0, 10))
        );

        assertEquals(ErrorCode.ONLY_OFFICIAL_LOUNGE_MEMBER_CAN_VIEW_INVITE_MATES, ex.getStatusCode());
        assertEquals("공식 라운지에 입실한 사람만 초대 가능한 메이트를 조회할 수 있습니다.", ex.getMessage());
    }

    @Test
    void inviteMate_requiresInviterToBeGroupMember() {
        UUID inviterId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        User inviter = User.builder()
                .name("inviter")
                .email("inviter@example.com")
                .build();
        ReflectionTestUtils.setField(inviter, "id", inviterId);

        User invitee = User.builder()
                .name("invitee")
                .email("invitee@example.com")
                .build();
        ReflectionTestUtils.setField(invitee, "id", inviteeId);

        Group group = Group.builder()
                .name("study")
                .build();
        ReflectionTestUtils.setField(group, "id", groupId);

        when(userRepository.findById(inviterId)).thenReturn(Optional.of(inviter));
        when(userRepository.findById(inviteeId)).thenReturn(Optional.of(invitee));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userGroupRepository.findByUserAndGroup(inviter, group)).thenReturn(Optional.empty());

        InviteMateRequest request = new InviteMateRequest();
        ReflectionTestUtils.setField(request, "inviteeId", inviteeId);

        com.mogakjak.mogakjak.global.exception.CustomException ex = assertThrows(
                com.mogakjak.mogakjak.global.exception.CustomException.class,
                () -> groupService.inviteMate(groupId, request, inviterId)
        );
        assertEquals(ErrorCode.ONLY_GROUP_MEMBER_CAN_INVITE, ex.getStatusCode());
        assertEquals("그룹 멤버만 초대할 수 있습니다.", ex.getMessage());
    }

    @Test
    void inviteMate_allowsOfficialLoungeWhenEntered() {
        UUID inviterId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        User inviter = User.builder()
                .name("inviter")
                .email("inviter@example.com")
                .build();
        ReflectionTestUtils.setField(inviter, "id", inviterId);

        User invitee = User.builder()
                .name("invitee")
                .email("invitee@example.com")
                .build();
        ReflectionTestUtils.setField(invitee, "id", inviteeId);

        Group lounge = Group.builder()
                .name("모각작 공식 라운지")
                .build();
        ReflectionTestUtils.setField(lounge, "id", groupId);
        ReflectionTestUtils.setField(lounge, "isOfficialLounge", true);

        when(userRepository.findById(inviterId)).thenReturn(Optional.of(inviter));
        when(userRepository.findById(inviteeId)).thenReturn(Optional.of(invitee));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(lounge));
        when(officialLoungeService.isEntered(inviterId)).thenReturn(true);
        when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(invitationRepository.existsByGroupAndInviteeAndStatus(lounge, invitee, com.mogakjak.mogakjak.domain.invitation.entity.InvitationStatus.PENDING))
                .thenReturn(false);

        InviteMateRequest request = new InviteMateRequest();
        ReflectionTestUtils.setField(request, "inviteeId", inviteeId);

        assertDoesNotThrow(() -> groupService.inviteMate(groupId, request, inviterId));
    }

    @Test
    void inviteMate_rejectsAlreadyEnteredOfficialLoungeMember() {
        UUID inviterId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        User inviter = User.builder()
                .name("inviter")
                .email("inviter@example.com")
                .build();
        ReflectionTestUtils.setField(inviter, "id", inviterId);

        User invitee = User.builder()
                .name("invitee")
                .email("invitee@example.com")
                .build();
        ReflectionTestUtils.setField(invitee, "id", inviteeId);

        Group lounge = Group.builder()
                .name("모각작 공식 라운지")
                .build();
        ReflectionTestUtils.setField(lounge, "id", groupId);
        ReflectionTestUtils.setField(lounge, "isOfficialLounge", true);

        when(userRepository.findById(inviterId)).thenReturn(Optional.of(inviter));
        when(userRepository.findById(inviteeId)).thenReturn(Optional.of(invitee));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(lounge));
        when(officialLoungeService.isEntered(inviterId)).thenReturn(true);
        when(officialLoungeService.isEntered(inviteeId)).thenReturn(true);

        InviteMateRequest request = new InviteMateRequest();
        ReflectionTestUtils.setField(request, "inviteeId", inviteeId);

        com.mogakjak.mogakjak.global.exception.CustomException ex = assertThrows(
                com.mogakjak.mogakjak.global.exception.CustomException.class,
                () -> groupService.inviteMate(groupId, request, inviterId)
        );

        assertEquals(ErrorCode.ALREADY_IN_OFFICIAL_LOUNGE, ex.getStatusCode());
        assertEquals("이미 공식 라운지에 입실한 사용자입니다.", ex.getMessage());
    }

    @Test
    void acceptInvitation_entersOfficialLoungeWithoutUserGroupMembership() {
        UUID invitationId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        UUID inviterId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        User invitee = User.builder()
                .name("invitee")
                .email("invitee@example.com")
                .build();
        ReflectionTestUtils.setField(invitee, "id", inviteeId);

        User inviter = User.builder()
                .name("inviter")
                .email("inviter@example.com")
                .build();
        ReflectionTestUtils.setField(inviter, "id", inviterId);

        Group lounge = Group.builder()
                .name("모각작 공식 라운지")
                .build();
        ReflectionTestUtils.setField(lounge, "id", groupId);
        ReflectionTestUtils.setField(lounge, "isOfficialLounge", true);

        Invitation invitation = Invitation.builder()
                .group(lounge)
                .inviter(inviter)
                .invitee(invitee)
                .status(InvitationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(invitation, "id", invitationId);

        when(userRepository.findById(inviteeId)).thenReturn(Optional.of(invitee));
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        groupService.acceptInvitation(invitationId, inviteeId);

        verify(officialLoungeService).enter(inviteeId);
        verify(userGroupRepository, never()).save(any());
        verify(groupMemberStatusService, never()).broadcastAllMemberStatuses(groupId);
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
    }

    @Test
    void acceptInvitation_skipsUserGroupCreationWhenAlreadyGroupMember() {
        UUID invitationId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        UUID inviterId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        User invitee = User.builder()
                .name("invitee")
                .email("invitee@example.com")
                .build();
        ReflectionTestUtils.setField(invitee, "id", inviteeId);

        User inviter = User.builder()
                .name("inviter")
                .email("inviter@example.com")
                .build();
        ReflectionTestUtils.setField(inviter, "id", inviterId);

        Group group = Group.builder()
                .name("study")
                .build();
        ReflectionTestUtils.setField(group, "id", groupId);

        UserGroup existingUserGroup = UserGroup.builder()
                .user(invitee)
                .group(group)
                .role(GroupRole.MEMBER)
                .build();

        Invitation invitation = Invitation.builder()
                .group(group)
                .inviter(inviter)
                .invitee(invitee)
                .status(InvitationStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(invitation, "id", invitationId);

        when(userRepository.findById(inviteeId)).thenReturn(Optional.of(invitee));
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(userGroupRepository.findByUserAndGroup(invitee, group)).thenReturn(Optional.of(existingUserGroup));

        groupService.acceptInvitation(invitationId, inviteeId);

        verify(userGroupRepository, never()).save(any());
        verify(groupMemberStatusService, never()).broadcastAllMemberStatuses(groupId);
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
    }

    @Test
    void inviteMate_requiresOfficialLoungeEntry() {
        UUID inviterId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        User inviter = User.builder()
                .name("inviter")
                .email("inviter@example.com")
                .build();
        ReflectionTestUtils.setField(inviter, "id", inviterId);

        User invitee = User.builder()
                .name("invitee")
                .email("invitee@example.com")
                .build();
        ReflectionTestUtils.setField(invitee, "id", inviteeId);

        Group lounge = Group.builder()
                .name("모각작 공식 라운지")
                .build();
        ReflectionTestUtils.setField(lounge, "id", groupId);
        ReflectionTestUtils.setField(lounge, "isOfficialLounge", true);

        when(userRepository.findById(inviterId)).thenReturn(Optional.of(inviter));
        when(userRepository.findById(inviteeId)).thenReturn(Optional.of(invitee));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(lounge));
        when(officialLoungeService.isEntered(inviterId)).thenReturn(false);

        InviteMateRequest request = new InviteMateRequest();
        ReflectionTestUtils.setField(request, "inviteeId", inviteeId);

        com.mogakjak.mogakjak.global.exception.CustomException ex = assertThrows(
                com.mogakjak.mogakjak.global.exception.CustomException.class,
                () -> groupService.inviteMate(groupId, request, inviterId)
        );

        assertEquals(ErrorCode.ONLY_OFFICIAL_LOUNGE_MEMBER_CAN_INVITE, ex.getStatusCode());
        assertEquals("공식 라운지에 입실한 사람만 초대할 수 있습니다.", ex.getMessage());
    }

    private static SharedGroupNameProjection sharedGroupName(UUID mateId, String groupName) {
        return new SharedGroupNameProjection() {
            @Override
            public UUID getMateId() {
                return mateId;
            }

            @Override
            public String getGroupName() {
                return groupName;
            }
        };
    }
}
