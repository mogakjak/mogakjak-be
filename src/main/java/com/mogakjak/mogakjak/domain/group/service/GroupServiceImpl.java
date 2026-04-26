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
import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.ImageCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserCharacterRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.domain.user.repository.projection.SharedGroupNameProjection;
import com.mogakjak.mogakjak.domain.user.entity.UserCharacter;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import com.mogakjak.mogakjak.domain.lounge.dto.OfficialLoungeSummaryResponse;
import com.mogakjak.mogakjak.domain.lounge.service.OfficialLoungeService;
import com.mogakjak.mogakjak.global.websocket.service.CheerNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.FocusNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.GroupMemberStatusService;
import com.mogakjak.mogakjak.global.websocket.service.GroupTimerService;
import com.mogakjak.mogakjak.global.websocket.service.InvitationNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.InvitationResponseNotificationService;
import com.mogakjak.mogakjak.global.websocket.service.PokeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GroupServiceImpl implements GroupService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;
    private final InvitationRepository invitationRepository;
    private final FocusNotificationService focusNotificationService;
    private final GroupMemberStatusService groupMemberStatusService;
    private final PokeNotificationService pokeNotificationService;
    private final CheerNotificationService cheerNotificationService;
    private final InvitationNotificationService invitationNotificationService;
    private final InvitationResponseNotificationService invitationResponseNotificationService;
    private final OfficialLoungeService officialLoungeService;
    private final UserCharacterRepository userCharacterRepository;
    private final ImageCharacterRepository imageCharacterRepository;
    private final GroupTimerService groupTimerService;

    @Override
    @Transactional(readOnly = true)
    public List<MyGroupResponse> getMyGroups(UUID userId) {
        User user = findUserById(userId);
        OfficialLoungeSummaryResponse officialLoungeSummary = officialLoungeService.getSummary(userId);

        List<UserGroup> myUserGroups = userGroupRepository.findAllByUserWithGroup(user);

        List<MyGroupResponse> response = new java.util.ArrayList<>();
        response.add(MyGroupResponse.fromOfficialLounge(officialLoungeSummary));
        response.addAll(myUserGroups.stream()
                .map(myUg -> {
                    Group group = myUg.getGroup();
                    List<MyGroupResponse.GroupMemberDto> members =
                            userGroupRepository.findByGroupIdWithUserAndProfile(group.getId()).stream()
                                    .map(ug -> {
                                        User member = ug.getUser();
                                        return MyGroupResponse.GroupMemberDto.builder()
                                                .userId(member.getId())
                                                .nickname(member.getName())
                                                .profileUrl(getProfileUrlFromUser(member))
                                                .level(getLevelFromUser(member))
                                                .role(ug.getRole())
                                                .build();
                                    }).collect(Collectors.toList());

                    return MyGroupResponse.fromGroup(group.getId(), group.getName(), group.getImageUrl(), members);
                })
                .collect(Collectors.toList()));

        return response;
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
                .role(userGroup.getRole())
                .build();

        return GroupDetailResponse.from(group, List.of(hostInfo));
    }

    @Override
    @Transactional
    public GroupDetailResponse getGroupDetail(UUID groupId, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        if (Boolean.TRUE.equals(group.getIsOfficialLounge())) {
            OfficialLoungeSummaryResponse officialLoungeSummary = officialLoungeService.getSummary(userId);
            List<GroupDetailResponse.MemberInfo> members = officialLoungeSummary.getMembers().stream()
                    .map(member -> GroupDetailResponse.MemberInfo.builder()
                            .userId(member.getUserId())
                            .nickname(member.getNickname())
                            .profileUrl(member.getProfileUrl())
                            .level(member.getLevel())
                            .role(null)
                            .build())
                    .collect(Collectors.toList());

            return GroupDetailResponse.from(group, members);
        }

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
                                    .profileUrl(getCharacterUrlFromUser(member))
                                    .level(getLevelFromUser(member)) // 레벨 정보 포함
                                    .role(ug.getRole())
                                    .build();
                        }).collect(Collectors.toList());

        return GroupDetailResponse.from(group, members);
    }

    @Override
    @Transactional
    public GroupDetailResponse updateGroup(UUID groupId, UpdateGroupRequest request, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        UserGroup userGroup = findUserGroup(user, group);
        // 방장 권한 체크
        if (userGroup.getRole() != GroupRole.HOST) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

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
                                    .role(ug.getRole())
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
                return MateResponse.from(mate, getLevelFromUser(mate), sharedGroupNames);
            });
        } else {
            Group group = findGroupById(groupId);
            checkUserInGroup(user, group);

            Page<User> users = userGroupRepository.findMatesByGroup(group, user, search, pageable);

            return users.map(u -> MateResponse.from(u, getLevelFromUser(u), List.of(group.getName())));
        }
    }

    @Override
    @Transactional
    public void leaveGroup(UUID groupId, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);
        UserGroup currentUserGroup = findUserGroup(user, group);

        if (currentUserGroup.getRole() == GroupRole.HOST) {
            userGroupRepository.findTopByGroupAndUserNotOrderByCreatedAtAsc(group, user)
                    .ifPresentOrElse(
                            nextHostUserGroup -> {
                                nextHostUserGroup.designateAsNewHost();
                                userGroupRepository.delete(currentUserGroup);
                                groupMemberStatusService.broadcastAllMemberStatuses(groupId);
                            },
                            () -> {
                                userGroupRepository.delete(currentUserGroup);
                                groupRepository.delete(group);
                            }
                    );
        } else {
            userGroupRepository.delete(currentUserGroup);
            groupMemberStatusService.broadcastAllMemberStatuses(groupId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public HostAckResponse getHostAckStatus(UUID groupId, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);
        UserGroup userGroup = findUserGroup(user, group);

        boolean needsAck = (userGroup.getRole() == GroupRole.HOST &&
                userGroup.getHostAckState() != null &&
                userGroup.getHostAckState() == 0);

        return HostAckResponse.builder()
                .needsAcknowledgment(needsAck)
                .build();
    }

    @Override
    @Transactional
    public void acknowledgeNewHost(UUID groupId, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);
        UserGroup userGroup = findUserGroup(user, group);

        if (userGroup.getRole() == GroupRole.HOST && userGroup.getHostAckState() == 0) {
            userGroup.acknowledgeHost(); // 1로 업데이트
        }
    }

    @Override
    @Transactional
    public void deleteGroupByHost(UUID groupId, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);
        UserGroup userGroup = findUserGroup(user, group);

        if (userGroup.getRole() != GroupRole.HOST) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        userGroupRepository.delete(userGroup);
        groupRepository.delete(group);
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
    public void ejectMemberFromGroup(UUID groupId, UUID targetUserId, UUID userId) {
        // 권한 확인 - 방장만 강퇴하도록 제한
        User user = findUserById(userId);
        Group group = findGroupById(groupId);
        UserGroup userGroup = findUserGroup(user, group);

        if (userGroup.getRole() != GroupRole.HOST) throw new CustomException(ErrorCode.FORBIDDEN);;

        leaveGroup(groupId, targetUserId);
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

        ensureCanInvite(inviter, group);

        if (Boolean.TRUE.equals(group.getIsOfficialLounge())) {
            if (officialLoungeService.isEntered(invitee.getId())) {
                throw new CustomException(ErrorCode.ALREADY_IN_OFFICIAL_LOUNGE);
            }
        } else if (userGroupRepository.findByUserAndGroup(invitee, group).isPresent()) {
            throw new CustomException(ErrorCode.ALREADY_GROUP_MEMBER);
        }

        if (invitationRepository.existsByGroupAndInviteeAndStatus(group, invitee, InvitationStatus.PENDING)) {
            throw new CustomException(ErrorCode.ALREADY_INVITED);
        }

        Invitation invitation = Invitation.builder()
                .group(group)
                .inviter(inviter)
                .invitee(invitee)
                .status(InvitationStatus.PENDING)
                .build();
        Invitation saved = invitationRepository.save(invitation);

        // 초대된 상대방에게 실시간 알림 전송
        invitationNotificationService.sendInvitationNotification(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> getMyInvitations(UUID userId) {
        User user = findUserById(userId);
        List<Invitation> invitations = invitationRepository.findByInviteeAndStatus(user, InvitationStatus.PENDING);

        // 그룹별 카운트 쿼리 중복 방지
        java.util.Map<java.util.UUID, long[]> groupCounts = new java.util.HashMap<>();
        for (Invitation inv : invitations) {
            java.util.UUID groupId = inv.getGroup().getId();
            if (!groupCounts.containsKey(groupId)) {
                long memberCount = userGroupRepository.countByGroup(inv.getGroup());
                long activeMemberCount = userGroupRepository.countActiveByGroup(inv.getGroup(), GroupParticipationStatus.NOT_PARTICIPATING);
                groupCounts.put(groupId, new long[]{memberCount, activeMemberCount});
            }
        }

        return invitations.stream()
                .map(inv -> {
                    long[] counts = groupCounts.get(inv.getGroup().getId());
                    long memberCount = counts != null ? counts[0] : 0L;
                    long activeMemberCount = counts != null ? counts[1] : 0L;
                    return InvitationResponse.from(inv, memberCount, activeMemberCount);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InviteMateResponse> getInviteMates(UUID userId, UUID groupId, String search, Pageable pageable) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);
        ensureCanViewInviteMates(user, group);

        Page<User> matePage = userGroupRepository.findTotalMatesByUser(user, search, pageable);
        List<User> mates = matePage.getContent();
        List<UUID> mateIds = mates.stream()
                .map(User::getId)
                .toList();
        Map<UUID, UserCharacter> topUserCharacterByUserId = loadTopUserCharacterByUserId(mateIds);
        ImageCharacter defaultImageCharacter = imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1)
                .orElse(null);
        Map<UUID, List<String>> sharedGroupNamesByMateId = loadSharedGroupNamesByMateId(user, mateIds);
        Set<UUID> groupMemberIds = loadGroupMemberIds(group);
        Set<UUID> pendingInviteeIds = loadPendingInviteeIds(group, mateIds);

        return matePage.map(mate -> {
            String profileUrl = getProfileUrlFromUser(mate, topUserCharacterByUserId, defaultImageCharacter);
            Integer level = getLevelFromUser(mate, topUserCharacterByUserId);
            List<String> sharedGroupNames = sharedGroupNamesByMateId.getOrDefault(mate.getId(), List.of());
            InviteMateStatus inviteStatus = resolveInviteMateStatus(group, mate.getId(), groupMemberIds, pendingInviteeIds);
            return InviteMateResponse.from(mate, profileUrl, level, sharedGroupNames, inviteStatus);
        });
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

        invitation.accept();

        if (Boolean.TRUE.equals(invitation.getGroup().getIsOfficialLounge())) {
            officialLoungeService.enter(user.getId());
        } else {
            if (userGroupRepository.findByUserAndGroup(user, invitation.getGroup()).isPresent()) {
                throw new CustomException(ErrorCode.ALREADY_GROUP_MEMBER);
            }

            UserGroup userGroup = UserGroup.create(user, invitation.getGroup(), GroupRole.MEMBER);
            userGroupRepository.save(userGroup);

            // 새 멤버 추가 시 전체 멤버 상태 브로드캐스트 (멤버 목록 변경)
            groupMemberStatusService.broadcastAllMemberStatuses(invitation.getGroup().getId());
        }

        // 초대한 사람에게 "수락됨" 알림 전송
        invitationResponseNotificationService.sendInvitationResponse(invitation, "ACCEPTED");
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

        // 초대한 사람에게 "거절됨" 알림 전송
        invitationResponseNotificationService.sendInvitationResponse(invitation, "DECLINED");
    }

    @Override
    public FocusNotificationResponse getFocusNotification(UUID userId, UUID groupId) {
        Group group = findGroupById(groupId);

        // User user = findUserById(user); // TODO: 이런 유효성 검사가 연관관계 무한루프 떄문에 안됨. 무한로딩됨 ㅠㅠ
        // findUserGroup(user, group);

        return FocusNotificationResponse.from(group);
    }

    @Override
    @Transactional
    public FocusNotificationResponse modifyFocusNotification(User user, UUID groupId, FocusNotificationRequest request) {
        Group group = findGroupById(groupId);

        UserGroup userGroup = findUserGroup(user, group);
        // 방장 권한 체크
        if (userGroup.getRole() != GroupRole.HOST) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

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
        log.info("Finding group with id: {}", groupId);
        return groupRepository.findById(groupId)
                .orElseThrow(() -> {
                    log.error("Group not found with id: {}", groupId);
                    return new CustomException(ErrorCode.GROUP_NOT_FOUND);
                });
    }

    private Invitation findInvitationById(UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITATION_NOT_FOUND));
    }

    private Map<UUID, UserCharacter> loadTopUserCharacterByUserId(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, UserCharacter> topUserCharacterByUserId = new HashMap<>();
        userCharacterRepository.findAllByUserIdInOrderByUserIdAscImageCharacter_LevelDescImageCharacter_CreatedAtAsc(userIds)
                .forEach(userCharacter -> topUserCharacterByUserId.putIfAbsent(userCharacter.getUser().getId(), userCharacter));
        return topUserCharacterByUserId;
    }

    private Map<UUID, List<String>> loadSharedGroupNamesByMateId(User me, List<UUID> mateIds) {
        if (mateIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<String>> sharedGroupNamesByMateId = new HashMap<>();
        userGroupRepository.findSharedGroupNamesByMates(me, mateIds).forEach(sharedGroupName -> {
            sharedGroupNamesByMateId
                    .computeIfAbsent(sharedGroupName.getMateId(), key -> new ArrayList<>())
                    .add(sharedGroupName.getGroupName());
        });
        return sharedGroupNamesByMateId;
    }

    private Set<UUID> loadGroupMemberIds(Group group) {
        return userGroupRepository.findAllByGroupWithUser(group).stream()
                .map(userGroup -> userGroup.getUser().getId())
                .collect(Collectors.toSet());
    }

    private Set<UUID> loadPendingInviteeIds(Group group, List<UUID> mateIds) {
        if (mateIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(invitationRepository.findInviteeIdsByGroupAndStatusAndInviteeIds(
                group,
                mateIds,
                InvitationStatus.PENDING
        ));
    }

    private InviteMateStatus resolveInviteMateStatus(
            Group group,
            UUID inviteeId,
            Set<UUID> groupMemberIds,
            Set<UUID> pendingInviteeIds
    ) {
        if (Boolean.TRUE.equals(group.getIsOfficialLounge())) {
            if (officialLoungeService.isEntered(inviteeId)) {
                return InviteMateStatus.ALREADY_IN_OFFICIAL_LOUNGE;
            }
        } else if (groupMemberIds.contains(inviteeId)) {
            return InviteMateStatus.ALREADY_GROUP_MEMBER;
        }

        if (pendingInviteeIds.contains(inviteeId)) {
            return InviteMateStatus.ALREADY_INVITED;
        }

        return InviteMateStatus.CAN_INVITE;
    }

    private Integer getLevelFromUser(User user, Map<UUID, UserCharacter> topUserCharacterByUserId) {
        return topUserCharacterByUserId.get(user.getId()) != null
                ? topUserCharacterByUserId.get(user.getId()).getImageCharacter().getLevel()
                : 1;
    }

    private String getProfileUrlFromUser(
            User user,
            Map<UUID, UserCharacter> topUserCharacterByUserId,
            ImageCharacter defaultImageCharacter
    ) {
        if (user.getImageUrl() != null) {
            return user.getImageUrl();
        }

        UserCharacter userCharacter = topUserCharacterByUserId.get(user.getId());
        if (userCharacter != null) {
            return userCharacter.getImageCharacter().getImageUrl();
        }

        return defaultImageCharacter != null ? defaultImageCharacter.getImageUrl() : null;
    }

    private UserGroup findUserGroup(User user, Group group) {
        return userGroupRepository.findByUserAndGroup(user, group)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
    }

    private void ensureCanInvite(User user, Group group) {
        if (Boolean.TRUE.equals(group.getIsOfficialLounge())) {
            if (!officialLoungeService.isEntered(user.getId())) {
                throw new CustomException(ErrorCode.ONLY_OFFICIAL_LOUNGE_MEMBER_CAN_INVITE);
            }
            return;
        }

        if (userGroupRepository.findByUserAndGroup(user, group).isEmpty()) {
            throw new CustomException(ErrorCode.ONLY_GROUP_MEMBER_CAN_INVITE);
        }
    }

    private void ensureCanViewInviteMates(User user, Group group) {
        if (Boolean.TRUE.equals(group.getIsOfficialLounge())) {
            if (!officialLoungeService.isEntered(user.getId())) {
                throw new CustomException(ErrorCode.ONLY_OFFICIAL_LOUNGE_MEMBER_CAN_VIEW_INVITE_MATES);
            }
            return;
        }

        if (userGroupRepository.findByUserAndGroup(user, group).isEmpty()) {
            throw new CustomException(ErrorCode.ONLY_GROUP_MEMBER_CAN_VIEW_INVITE_MATES);
        }
    }

    private UserGroup checkUserInGroup(User user, Group group) {
        return userGroupRepository.findByUserAndGroup(user, group)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_GROUP_MEMBER));
    }

    private Integer getLevelFromUser(User user) {
        return userCharacterRepository.findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(user)
                .map(uc -> uc.getImageCharacter().getLevel())
                .orElse(1);
    }

    private String getProfileUrlFromUser(User user) {
        if (user.getImageUrl() != null) {
            return user.getImageUrl();
        }
        return userCharacterRepository.findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(user)
                .map(uc -> uc.getImageCharacter().getImageUrl())
                .orElseGet(() -> {
                    return imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1)
                            .map(ImageCharacter::getImageUrl)
                            .orElse(null);
                });
    }

    private String getCharacterUrlFromUser(User user) {
        return userCharacterRepository.findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(user)
                .map(uc -> uc.getImageCharacter().getImageUrl())
                .orElseGet(() -> {
                    return imageCharacterRepository.findFirstByLevelAndIsActiveTrueOrderByCreatedAtAsc(1)
                            .map(ImageCharacter::getImageUrl)
                            .orElse(null);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public GroupNameResponse getGroupName(UUID groupId) {
        log.info("getGroupName called with groupId: {}", groupId);
        Group group = findGroupById(groupId);
        return GroupNameResponse.builder()
                .groupName(group.getName())
                .build();
    }
}
