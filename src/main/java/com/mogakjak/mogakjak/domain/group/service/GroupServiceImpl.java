package com.mogakjak.mogakjak.domain.group.service;

import com.mogakjak.mogakjak.domain.group.controller.dto.*;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.invitation.entity.Invitation;
import com.mogakjak.mogakjak.domain.invitation.entity.InvitationStatus;
import com.mogakjak.mogakjak.domain.invitation.repository.InvitationRepository;
import com.mogakjak.mogakjak.domain.invitation.controller.dto.*;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.GroupRole;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.global.websocket.service.CheerNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.FocusNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.GroupMemberStatusService;
import com.mogakjak.mogakjak.global.websocket.service.PokeNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupServiceImpl implements GroupService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;
    private final InvitationRepository invitationRepository;
    private final FocusNotificationService focusNotificationService;
    private final GroupMemberStatusService groupMemberStatusService;
    private final PokeNotificationService pokeNotificationService;
    private final CheerNotificationService cheerNotificationService;

    @Override
    @Transactional(readOnly = true)
    public List<MyGroupResponse> getMyGroups(UUID userId) {
        User user = findUserById(userId);

        // 내가 가입한 그룹 목록 조회
        List<UserGroup> myUserGroups = userGroupRepository.findAllByUserWithGroup(user);

        return myUserGroups.stream().map(myUg -> {
            Group group = myUg.getGroup();

            // 해당 그룹의 멤버들을 프로필 정보(레벨, 이미지)와 함께 조회
            // (UserGroupRepository에 findByGroupIdWithUserAndProfile 메서드 추가 필요)
            List<MyGroupResponse.GroupMemberDto> members =
                    userGroupRepository.findByGroupIdWithUserAndProfile(group.getId()).stream()
                            .map(ug -> {
                                User member = ug.getUser();
                                return MyGroupResponse.GroupMemberDto.builder()
                                        .userId(member.getId())
                                        .nickname(member.getName())
                                        .profileUrl(getProfileUrlFromUser(member))
                                        .level(getLevelFromUser(member))
                                        .build();
                            }).collect(Collectors.toList());

            return MyGroupResponse.builder()
                    .groupId(group.getId())
                    .groupName(group.getName())
                    .imageUrl(group.getImageUrl()) // 그룹 이미지 URL 포함
                    .members(members)              // 그룹 멤버 리스트 포함
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GroupDetailResponse createGroup(CreateGroupRequest request, UUID userId) {
        User user = findUserById(userId);

        Group group = Group.builder()
                .name(request.getName())
                .imageUrl(request.getImageUrl())
                .description("")
                .password(null)
                .build();

        groupRepository.save(group);

        // 그룹 생성자를 HOST로 지정
        UserGroup userGroup = UserGroup.create(user, group, GroupRole.HOST);
        userGroupRepository.save(userGroup);

        // 생성된 그룹 정보 반환 시, 방장의 레벨 정보 등 포함
        GroupDetailResponse.MemberInfo hostInfo = GroupDetailResponse.MemberInfo.builder()
                .userId(user.getId())
                .nickname(user.getName())
                .profileUrl(getProfileUrlFromUser(user))
                .level(getLevelFromUser(user))
                .build();

        return GroupDetailResponse.from(group, List.of(hostInfo));
    }

    @Override
    @Transactional
    public GroupDetailResponse getGroupDetail(UUID groupId, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        UserGroup userGroup = checkUserInGroup(user, group);

        // 그룹 입장 처리: NOT_PARTICIPATING 상태이거나 null인 경우 입장 일시 기록 및 참여 상태를 휴식 중으로 설정
        if (userGroup.getParticipationStatus() == null || 
            userGroup.getParticipationStatus() == GroupParticipationStatus.NOT_PARTICIPATING) {
            userGroup.enterGroup(java.time.LocalDateTime.now());
            userGroupRepository.save(userGroup);
            
            // 그룹 멤버 상태 변경 브로드캐스트
            groupMemberStatusService.broadcastMemberStatusUpdate(groupId, userId);
        }

        // 그룹 멤버 조회 시 레벨과 프로필 이미지 포함
        List<GroupDetailResponse.MemberInfo> members =
                userGroupRepository.findByGroupIdWithUserAndProfile(groupId).stream()
                        .map(ug -> {
                            User member = ug.getUser();
                            return GroupDetailResponse.MemberInfo.builder()
                                    .userId(member.getId())
                                    .nickname(member.getName())
                                    .profileUrl(getProfileUrlFromUser(member))
                                    .level(getLevelFromUser(member)) // 레벨 정보 포함
                                    .build();
                        }).collect(Collectors.toList());

        return GroupDetailResponse.from(group, members);
    }

    @Override
    @Transactional
    public GroupDetailResponse updateGroup(UUID groupId, UpdateGroupRequest request, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        String newName = StringUtils.hasText(request.getName()) ? request.getName() : group.getName();
        String newImageUrl = StringUtils.hasText(request.getImageUrl()) ? request.getImageUrl() : group.getImageUrl();

        group.updateInfo(newName, newImageUrl);

        // 업데이트된 정보 반환을 위해 멤버 정보 다시 조회
        List<GroupDetailResponse.MemberInfo> members =
                userGroupRepository.findByGroupIdWithUserAndProfile(groupId).stream()
                        .map(ug -> {
                            User member = ug.getUser();
                            return GroupDetailResponse.MemberInfo.builder()
                                    .userId(member.getId())
                                    .nickname(member.getName())
                                    .profileUrl(getProfileUrlFromUser(member))
                                    .level(getLevelFromUser(member))
                                    .build();
                        }).collect(Collectors.toList());

        return GroupDetailResponse.from(group, members);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MateResponse> getMates(UUID userId, UUID groupId, String search, Pageable pageable) {
        User user = findUserById(userId);

        if (groupId == null) {
            Page<User> matePage = userGroupRepository.findTotalMatesByUser(user, search, pageable);

            return matePage.map(mate -> {
                List<String> sharedGroupNames = userGroupRepository.findSharedGroupNames(user, mate);
                return MateResponse.from(mate, sharedGroupNames);
            });
        } else {
            Group group = findGroupById(groupId);
            checkUserInGroup(user, group);

            Page<User> users = userGroupRepository.findMatesByGroup(group, user, search, pageable);

            return users.map(u -> MateResponse.from(u, List.of(group.getName())));
        }
    }

    @Override
    @Transactional
    public void leaveGroup(UUID groupId, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);
        UserGroup userGroup = findUserGroup(user, group);
        
        if (userGroup.getRole() == GroupRole.HOST) {
            long memberCount = userGroupRepository.countByGroup(group);
            if (memberCount > 1) {
                throw new CustomException(ErrorCode.CANNOT_LEAVE_AS_HOST);
            }
            userGroupRepository.delete(userGroup);
            groupRepository.delete(group);
            // 그룹이 삭제되면 브로드캐스트 불필요
        } else {
            userGroupRepository.delete(userGroup);
            // 멤버 탈퇴 시 전체 멤버 상태 브로드캐스트 (멤버 목록 변경)
            groupMemberStatusService.broadcastAllMemberStatuses(groupId);
        }
    }

    @Override
    @Transactional
    public void leaveGroupSession(UUID groupId, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);
        UserGroup userGroup = findUserGroup(user, group);

        // 그룹 세션에서 나가기: 참여 상태를 NOT_PARTICIPATING으로 변경 (멤버는 유지, enteredAt은 유지)
        userGroup.leaveGroupSession();
        userGroupRepository.save(userGroup);
        
        // 그룹 멤버 상태 변경 브로드캐스트
        groupMemberStatusService.broadcastMemberStatusUpdate(groupId, userId);
        
        // 모든 멤버가 NOT_PARTICIPATING이 되면 응원 수 및 누적 시간 초기화
        resetAllCheerCounts(groupId);
        resetAccumulatedDuration(groupId);
    }

    @Override
    @Transactional
    public void sendCheer(UUID userId, UUID groupId, UUID targetUserId) {
        User user = findUserById(userId);
        User targetUser = findUserById(targetUserId);
        Group group = findGroupById(groupId);

        if (userId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_INVITE_SELF);
        }

        // 두 사용자가 모두 해당 그룹의 멤버인지 확인
        UserGroup myUserGroup = userGroupRepository.findByUser_IdAndGroup_Id(userId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
        
        UserGroup targetUserGroup = userGroupRepository.findByUser_IdAndGroup_Id(targetUserId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));

        // 발신자는 NOT_PARTICIPATING이 아니어야 함
        if (myUserGroup.getParticipationStatus() == GroupParticipationStatus.NOT_PARTICIPATING) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 수신자는 NOT_PARTICIPATING이 아니어야 함
        if (targetUserGroup.getParticipationStatus() == GroupParticipationStatus.NOT_PARTICIPATING) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 응원 수 증가
        targetUserGroup.incrementCheerCount();
        userGroupRepository.save(targetUserGroup);

        // 응원 알림 전송
        cheerNotificationService.sendCheerNotification(userId, targetUserId, groupId);

        // 그룹 멤버 상태 브로드캐스트 (응원 수 업데이트 반영)
        groupMemberStatusService.broadcastAllMemberStatuses(groupId);
    }

    @Override
    @Transactional
    public void resetAllCheerCounts(UUID groupId) {
        Group group = findGroupById(groupId);
        List<UserGroup> userGroups = userGroupRepository.findAllByGroupWithUser(group);

        // 모든 멤버가 NOT_PARTICIPATING인지 확인
        boolean allNotParticipating = userGroups.stream()
                .allMatch(ug -> ug.getParticipationStatus() == GroupParticipationStatus.NOT_PARTICIPATING);

        if (allNotParticipating) {
            // 모든 멤버의 응원 수 초기화
            userGroups.forEach(UserGroup::resetCheerCount);
            userGroupRepository.saveAll(userGroups);
            
            // 그룹 타이머 누적 시간 초기화
            group.resetAccumulatedDuration();
            groupRepository.save(group);

            // 그룹 멤버 상태 브로드캐스트
            groupMemberStatusService.broadcastAllMemberStatuses(groupId);
        }
    }

    @Override
    @Transactional
    public void addGroupAccumulatedDuration(UUID groupId, Long seconds) {
        Group group = findGroupById(groupId);
        group.addAccumulatedDuration(seconds);
        groupRepository.save(group);
    }

    @Override
    @Transactional
    public void resetAccumulatedDuration(UUID groupId) {
        Group group = findGroupById(groupId);
        List<UserGroup> userGroups = userGroupRepository.findAllByGroupWithUser(group);

        // 모든 멤버가 NOT_PARTICIPATING인지 확인
        boolean allNotParticipating = userGroups.stream()
                .allMatch(ug -> ug.getParticipationStatus() == GroupParticipationStatus.NOT_PARTICIPATING);

        if (allNotParticipating) {
            group.resetAccumulatedDuration();
            groupRepository.save(group);
        }
    }

    @Override
    @Transactional
    public void inviteMate(UUID groupId, InviteMateRequest request, UUID inviterId) {
        User inviter = findUserById(inviterId);
        User invitee = findUserById(request.getInviteeId());
        Group group = findGroupById(groupId);

        if (inviter.getId().equals(invitee.getId())) {
            throw new CustomException(ErrorCode.CANNOT_INVITE_SELF);
        }

        if (userGroupRepository.findByUserAndGroup(invitee, group).isPresent()) {
            throw new CustomException(ErrorCode.ALREADY_GROUP_MEMBER);
        }

        invitationRepository.findByGroupAndInvitee(group, invitee)
                .ifPresent(invitation -> {
                    if (invitation.getStatus() == InvitationStatus.PENDING) {
                        throw new CustomException(ErrorCode.ALREADY_INVITED);
                    }
                });

        Invitation invitation = Invitation.builder()
                .group(group)
                .inviter(inviter)
                .invitee(invitee)
                .status(InvitationStatus.PENDING)
                .build();
        invitationRepository.save(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> getMyInvitations(UUID userId) {
        User user = findUserById(userId);
        return invitationRepository.findByInviteeAndStatus(user, InvitationStatus.PENDING).stream()
                .map(InvitationResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void acceptInvitation(UUID invitationId, UUID userId) {
        User user = findUserById(userId);
        Invitation invitation = findInvitationById(invitationId);

        if (!invitation.getInvitee().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INVITATION);
        }

        if (userGroupRepository.findByUserAndGroup(user, invitation.getGroup()).isPresent()) {
            invitation.accept();
            throw new CustomException(ErrorCode.ALREADY_GROUP_MEMBER);
        }

        invitation.accept();

        UserGroup userGroup = UserGroup.create(user, invitation.getGroup(), GroupRole.MEMBER);
        userGroupRepository.save(userGroup);
        
        // 새 멤버 추가 시 전체 멤버 상태 브로드캐스트 (멤버 목록 변경)
        groupMemberStatusService.broadcastAllMemberStatuses(invitation.getGroup().getId());
    }

    @Override
    @Transactional
    public void declineInvitation(UUID invitationId, UUID userId) {
        User user = findUserById(userId);
        Invitation invitation = findInvitationById(invitationId);

        if (!invitation.getInvitee().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INVITATION);
        }

        invitation.decline();
    }

    // === 편의 메서드 ===
    @Override
    @Transactional
    public FocusNotificationResponse modifyFocusNotification(User user, UUID groupId, FocusNotificationRequest request) {
        Group group = findGroupById(groupId);

        // 유저가 그룹에 접근 권한이 있는지 확인
        findUserGroup(user, group);

        group.updateFocusNotificationInfo(
                request.isNotificationAgreed(),
                request.notificationCycle(),
                request.notificationMessage()
        );

        return FocusNotificationResponse.from(group);
    }

    @Override
    @Transactional
    public GroupGoalResponse setGroupGoal(User user, UUID groupId, GroupGoalRequest request) {
        Group group = findGroupById(groupId);
        findUserGroup(user, group);

        int totalSeconds = (request.hour() * 3600) + (request.minute() * 60);

        group.updateGoalSeconds(totalSeconds);

        return GroupGoalResponse.from(group);
    }

    @Override
    @Transactional
    public void testSendFocusNotification(User user, UUID groupId) {
        Group group = findGroupById(groupId);
        findUserGroup(user, group); // 그룹 멤버인지 확인
        
        // 테스트용: 알림 동의 여부와 활동 중인 사용자 여부를 무시하고 강제 전송
        focusNotificationService.sendTestNotification(groupId);
    }

    @Override
    public String createInvitationUrl(UUID groupId, UUID userId, String frontendBaseUrl) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        checkUserInGroup(user, group);

        return String.format("%s/invite/%s", frontendBaseUrl, groupId);
    }

    @Override
    public void joinGroupViaLink(UUID groupId, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        if (userGroupRepository.findByUserAndGroup(user, group).isPresent()) {
            throw new CustomException(ErrorCode.ALREADY_GROUP_MEMBER);
        }

        // 바로 멤버로 추가 (초대 수락 과정 없이 가입)
        UserGroup userGroup = UserGroup.create(user, group, GroupRole.MEMBER);
        userGroupRepository.save(userGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommonGroupResponse> getCommonGroups(UUID userId, UUID targetUserId) {
        User user = findUserById(userId);
        User targetUser = findUserById(targetUserId);

        if (userId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 두 사용자가 함께 있는 그룹 목록 조회
        List<Group> commonGroups = userGroupRepository.findCommonGroups(userId, targetUserId);

        return commonGroups.stream().map(group -> {
            // 현재 사용자의 그룹 참여 상태
            UserGroup myUserGroup = userGroupRepository.findByUser_IdAndGroup_Id(userId, group.getId())
                    .orElse(null);
            GroupParticipationStatus myStatus = myUserGroup != null 
                    ? myUserGroup.getParticipationStatus() 
                    : GroupParticipationStatus.NOT_PARTICIPATING;

            // 상대방의 그룹 참여 상태
            UserGroup targetUserGroup = userGroupRepository.findByUser_IdAndGroup_Id(targetUserId, group.getId())
                    .orElse(null);
            GroupParticipationStatus targetStatus = targetUserGroup != null 
                    ? targetUserGroup.getParticipationStatus() 
                    : GroupParticipationStatus.NOT_PARTICIPATING;

            // 그룹 멤버 수
            long memberCount = userGroupRepository.countByGroup(group);

            return CommonGroupResponse.builder()
                    .groupId(group.getId())
                    .groupName(group.getName())
                    .imageUrl(group.getImageUrl())
                    .memberCount(memberCount)
                    .maxMemberCount(8) // 기본값, 필요시 Group 엔티티에 필드 추가
                    .myParticipationStatus(myStatus)
                    .targetParticipationStatus(targetStatus)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void sendPokeNotification(UUID userId, UUID targetUserId, UUID groupId) {
        User user = findUserById(userId);
        User targetUser = findUserById(targetUserId);
        Group group = findGroupById(groupId);

        if (userId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_INVITE_SELF);
        }

        // 두 사용자가 모두 해당 그룹의 멤버인지 확인
        UserGroup myUserGroup = userGroupRepository.findByUser_IdAndGroup_Id(userId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
        
        UserGroup targetUserGroup = userGroupRepository.findByUser_IdAndGroup_Id(targetUserId, groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));

        // 콕 찌르기 알림 전송
        pokeNotificationService.sendPokeNotification(userId, targetUserId, groupId);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Group findGroupById(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
    }

    private Invitation findInvitationById(UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITATION_NOT_FOUND));
    }

    private UserGroup findUserGroup(User user, Group group) {
        return userGroupRepository.findByUserAndGroup(user, group)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
    }

    private UserGroup checkUserInGroup(User user, Group group) {
        return userGroupRepository.findByUserAndGroup(user, group)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
    }

    private Integer getLevelFromUser(User user) {
        if (user.getUserProfile() != null && user.getUserProfile().getMainImageCharacter() != null) {
            return user.getUserProfile().getMainImageCharacter().getLevel();
        }
        return 1;
    }

    private String getProfileUrlFromUser(User user) {
        // 사용자가 설정한 프로필 사진이 있으면 반환
        if (user.getImageUrl() != null) {
            return user.getImageUrl();
        }
        // 없으면 기존 로직대로 캐릭터 이미지 반환
        if (user.getUserProfile() != null && user.getUserProfile().getMainImageCharacter() != null) {
            return user.getUserProfile().getMainImageCharacter().getImageUrl();
        }
        return null;
    }

    private MyGroupResponse toMyGroupDto(Group group) {
        return MyGroupResponse.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .imageUrl(group.getImageUrl())
                .build();
    }
}