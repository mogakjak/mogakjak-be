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

    public static UserGroup create(User user, Group group, GroupRole role) {
        return UserGroup.builder()
                .user(user)
                .group(group)
                .role(role)
                .participationStatus(GroupParticipationStatus.NOT_PARTICIPATING)
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
}