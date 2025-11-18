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

    // 그룹으로 메이트 초대
    void inviteMate(UUID groupId, InviteMateRequest request, UUID inviterId);

    // 내가 받은 초대 목록 조회
    List<InvitationResponse> getMyInvitations(UUID userId);

    // 초대 수락
    void acceptInvitation(UUID invitationId, UUID userId);

    // 초대 거절
    void declineInvitation(UUID invitationId, UUID userId);
}