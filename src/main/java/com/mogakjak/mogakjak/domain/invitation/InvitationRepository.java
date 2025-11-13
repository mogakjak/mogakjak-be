package com.mogakjak.mogakjak.domain.invitation;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    // 내가 받은 초대 목록 조회 (대기 중인 것만)
    List<Invitation> findByInviteeAndStatus(User invitee, InvitationStatus status);

    // 이미 초대를 보냈는지 확인
    Optional<Invitation> findByGroupAndInvitee(Group group, User invitee);
}