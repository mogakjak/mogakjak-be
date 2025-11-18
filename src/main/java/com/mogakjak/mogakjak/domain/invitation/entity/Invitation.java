package com.mogakjak.mogakjak.domain.invitation.entity;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "invitation")
public class Invitation extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InvitationStatus status; // 초대의 현재 상태 (PENDING, ACCEPTED, DECLINED)

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
    }

    public void decline() {
        this.status = InvitationStatus.DECLINED;
    }
}