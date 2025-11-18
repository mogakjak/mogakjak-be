package com.mogakjak.mogakjak.domain.group.service;

import com.mogakjak.mogakjak.domain.group.controller.dto.*;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.repository.GroupRepository;
import com.mogakjak.mogakjak.domain.invitation.Invitation;
import com.mogakjak.mogakjak.domain.invitation.InvitationRepository;
import com.mogakjak.mogakjak.domain.invitation.InvitationResponse;
import com.mogakjak.mogakjak.domain.invitation.InvitationStatus;
import com.mogakjak.mogakjak.domain.invitation.InviteMateRequest;
import com.mogakjak.mogakjak.domain.user.entity.GroupRole;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
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
@Transactional(readOnly = true)
public class GroupServiceImpl implements GroupService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final GroupRepository groupRepository;
    private final InvitationRepository invitationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MyGroupResponse> getMyGroups(UUID userId) {
        User user = findUserById(userId);

        return userGroupRepository.findAllByUserWithGroup(user).stream()
                .map(userGroup -> toMyGroupDto(userGroup.getGroup()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GroupDetailResponse createGroup(CreateGroupRequest request, UUID userId) {
        User user = findUserById(userId);

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .password(null)
                .build();

        groupRepository.save(group);

        // 그룹 생성자를 HOST로 지정
        UserGroup userGroup = UserGroup.create(user, group, GroupRole.HOST);
        userGroupRepository.save(userGroup);

        GroupDetailResponse.MemberInfo hostInfo = GroupDetailResponse.MemberInfo.builder()
                .userId(user.getId())
                .nickname(user.getName())
                .build();

        // 생성된 그룹의 상세 정보 반환
        return GroupDetailResponse.from(group, List.of(hostInfo));
    }

    @Override
    @Transactional(readOnly = true)
    public GroupDetailResponse getGroupDetail(UUID groupId, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

//        checkUserInGroup(user, group);

        List<GroupDetailResponse.MemberInfo> members = userGroupRepository.findAllByGroupWithUser(group).stream()
                .map(ug -> GroupDetailResponse.MemberInfo.builder()
                        .userId(ug.getUser().getId())
                        .nickname(ug.getUser().getName())
                        .build())
                .collect(Collectors.toList());

        return GroupDetailResponse.from(group, members);
    }

    @Override
    @Transactional
    public GroupDetailResponse updateGroup(UUID groupId, UpdateGroupRequest request, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);

        // 방장(HOST)만 수정 가능
//        checkUserRole(user, group);

        if (StringUtils.hasText(request.getName())) {
             group.updateName(request.getName());
        }
        if (StringUtils.hasText(request.getDescription())) {
             group.updateDescription(request.getDescription());
        }

        List<GroupDetailResponse.MemberInfo> members = userGroupRepository.findAllByGroupWithUser(group).stream()
                .map(ug -> GroupDetailResponse.MemberInfo.builder()
                        .userId(ug.getUser().getId())
                        .nickname(ug.getUser().getName())
                        .build())
                .collect(Collectors.toList());

        return GroupDetailResponse.from(group, members);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MateResponse> getMates(UUID userId, UUID groupId, String search, Pageable pageable) {
        User user = findUserById(userId);
        Page<User> userPage;

        if (groupId == null) {
            userPage = userGroupRepository.findTotalMatesByUser(user, search, pageable);
        } else {
            Group group = findGroupById(groupId);
            checkUserInGroup(user, group);

            userPage = userGroupRepository.findMatesByGroup(group, user, search, pageable);
        }

        return userPage.map(MateResponse::from);
    }

    @Override
    @Transactional
    public void leaveGroup(UUID groupId, UUID userId) {
        User user = findUserById(userId);
        Group group = findGroupById(groupId);
        UserGroup userGroup = findUserGroup(user, group);

        if (userGroup.getRole() == GroupRole.HOST) {
            // 방장인 경우
            long memberCount = userGroupRepository.countByGroup(group);
            if (memberCount > 1) {
                // 다른 멤버가 있으면 탈퇴 불가
                throw new CustomException(ErrorCode.CANNOT_LEAVE_AS_HOST);
            }
            // 혼자 있으면 그룹 삭제
            userGroupRepository.delete(userGroup);
            groupRepository.delete(group);
        } else {
            // 일반 멤버인 경우
            userGroupRepository.delete(userGroup);
        }
    }

    @Override
    @Transactional
    public void inviteMate(UUID groupId, InviteMateRequest request, UUID inviterId) {
        User inviter = findUserById(inviterId);
        User invitee = findUserById(request.getInviteeId());
        Group group = findGroupById(groupId);

//        checkUserRole(inviter, group);

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

        // 이미 그룹 멤버인 경우
        if (userGroupRepository.findByUserAndGroup(user, invitation.getGroup()).isPresent()) {
            // 초대는 수락 처리하되, 멤버로 추가하지 않음
            invitation.accept();
            throw new CustomException(ErrorCode.ALREADY_GROUP_MEMBER);
        }

        invitation.accept();

        UserGroup userGroup = UserGroup.create(user, invitation.getGroup(), GroupRole.MEMBER);
        userGroupRepository.save(userGroup);
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

    private void checkUserInGroup(User user, Group group) {
        if (userGroupRepository.findByUserAndGroup(user, group).isEmpty()) {
            throw new CustomException(ErrorCode.NOT_GROUP_MEMBER);
        }
    }

//    private void checkUserRole(User user, Group group) {
//        if (!userGroupRepository.existsByUserAndGroupAndRole(user, group, GroupRole.HOST)) {
//            throw new CustomException(ErrorCode.FORBIDDEN);
//        }
//    }

    private MyGroupResponse toMyGroupDto(Group group) {
        return MyGroupResponse.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .build();
    }
}