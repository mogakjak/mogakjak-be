package com.mogakjak.mogakjak.domain.user.entity;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "group_id"})
})
public class UserGroup extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GroupRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GroupParticipationStatus participationStatus = GroupParticipationStatus.NOT_PARTICIPATING;

    private LocalDateTime enteredAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer cheerCount = 0;

    public static UserGroup create(User user, Group group, GroupRole role) {
        return UserGroup.builder()
                .user(user)
                .group(group)
                .role(role)
                .participationStatus(GroupParticipationStatus.NOT_PARTICIPATING)
                .cheerCount(0)
                .build();
    }

    public void enterGroup(LocalDateTime enteredAt) {
        this.enteredAt = enteredAt;
        this.participationStatus = GroupParticipationStatus.RESTING;
    }

    public void setParticipationStatus(GroupParticipationStatus status) {
        this.participationStatus = status;
    }

    public void leaveGroup() {
        this.enteredAt = null;
        this.participationStatus = GroupParticipationStatus.NOT_PARTICIPATING;
    }

    /**
     * 그룹 세션에서 나가기 (멤버는 유지, enteredAt은 유지)
     * enteredAt은 그룹 입장 이력을 위해 유지하고, participationStatus만 변경
     */
    public void leaveGroupSession() {
        this.participationStatus = GroupParticipationStatus.NOT_PARTICIPATING;
    }

    /**
     * 응원 수 증가
     */
    public void incrementCheerCount() {
        this.cheerCount++;
    }

    /**
     * 응원 수 초기화
     */
    public void resetCheerCount() {
        this.cheerCount = 0;
    }
}