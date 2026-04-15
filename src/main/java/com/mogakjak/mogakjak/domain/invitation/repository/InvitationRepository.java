package com.mogakjak.mogakjak.domain.invitation.repository;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.invitation.entity.Invitation;
import com.mogakjak.mogakjak.domain.invitation.entity.InvitationStatus;
import com.mogakjak.mogakjak.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    // 내가 받은 초대 목록 조회 (대기 중인 것만)
    @Query("SELECT i FROM Invitation i JOIN FETCH i.group JOIN FETCH i.inviter WHERE i.invitee = :invitee AND i.status = :status")
    List<Invitation> findByInviteeAndStatus(@Param("invitee") User invitee, @Param("status") InvitationStatus status);

    // 이미 초대를 보냈는지 확인
    @Query("SELECT i FROM Invitation i WHERE i.group = :group AND i.invitee = :invitee ORDER BY i.createdAt DESC")
    List<Invitation> findAllByGroupAndInvitee(@Param("group") Group group, @Param("invitee") User invitee);

    boolean existsByGroupAndInviteeAndStatus(Group group, User invitee, InvitationStatus status);
}
