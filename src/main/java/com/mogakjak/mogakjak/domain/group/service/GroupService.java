package com.mogakjak.mogakjak.domain.group.service;

import com.mogakjak.mogakjak.domain.group.controller.dto.CreateGroupRequest;
import com.mogakjak.mogakjak.domain.group.controller.dto.GroupDetailResponse;
import com.mogakjak.mogakjak.domain.group.controller.dto.MateResponse;
import com.mogakjak.mogakjak.domain.group.controller.dto.MyGroupResponse;
import com.mogakjak.mogakjak.domain.group.controller.dto.UpdateGroupRequest;
import com.mogakjak.mogakjak.domain.invitation.controller.dto.InvitationResponse;
import com.mogakjak.mogakjak.domain.invitation.controller.dto.InviteMateRequest;
import java.util.List;
import java.util.UUID;
import com.mogakjak.mogakjak.domain.group.controller.dto.*;
import com.mogakjak.mogakjak.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GroupService {

    // 내 그룹 목록 조회
    List<MyGroupResponse> getMyGroups(UUID userId);

    // 그룹 생성
    GroupDetailResponse createGroup(CreateGroupRequest request, UUID userId);

    // 그룹 상세 정보 조회
    GroupDetailResponse getGroupDetail(UUID groupId, UUID userId);

    // 그룹 정보 수정
    GroupDetailResponse updateGroup(UUID groupId, UpdateGroupRequest request, UUID userId);

    // 내 전체 또는 특정 그룹 메이트 조회
    Page<MateResponse> getMates(UUID userId, UUID groupId, String search, Pageable pageable);

    // 그룹 탈퇴
    void leaveGroup(UUID groupId, UUID userId);

    // 그룹 세션에서 나가기 (멤버는 유지, 참여 상태만 NOT_PARTICIPATING으로 변경)
    void leaveGroupSession(UUID groupId, UUID userId);

    // 그룹으로 메이트 초대
    void inviteMate(UUID groupId, InviteMateRequest request, UUID inviterId);

    // 내가 받은 초대 목록 조회
    List<InvitationResponse> getMyInvitations(UUID userId);

    // 초대 수락
    void acceptInvitation(UUID invitationId, UUID userId);

    // 초대 거절
    void declineInvitation(UUID invitationId, UUID userId);

    // 그룹 집중 체크 알림 설정
    FocusNotificationResponse modifyFocusNotification(User user, UUID groupId, FocusNotificationRequest request);

    // 그룹 공동 목표 설정
    GroupGoalResponse setGroupGoal(User user, UUID groupId, GroupGoalRequest request);

    // [테스트용] 집중 체크 알림 수동 전송
    void testSendFocusNotification(User user, UUID groupId);

    // 초대 링크 생성
    String createInvitationUrl(UUID groupId, UUID userId, String frontendBaseUrl);

    // 초대 링크를 통한 그룹 가입
    void joinGroupViaLink(UUID groupId, UUID userId);
}