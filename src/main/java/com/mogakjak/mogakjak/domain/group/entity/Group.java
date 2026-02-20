package com.mogakjak.mogakjak.domain.group.entity;

import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mogak_groups")
public class Group extends BaseSchema {

    @Column(nullable = false, length = 30)
    private String name;

    private String imageUrl;

    private String description;

    private String password;

    @Column(nullable = false)
    @Builder.Default
    private Integer goalSeconds = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isNotificationAgreed = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer notificationCycle = 1;

    @Column(nullable = false)
    @Builder.Default
    private String notificationMessage = "여전히 몰입 중이시네요. 멋져요!";

    private LocalDateTime lastNotificationSentAt;

    @Column(nullable = false)
    @Builder.Default
    private Long accumulatedDuration = 0L; // 그룹 타이머 누적 시간 (초 단위)

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserGroup> userGroups = new ArrayList<>();

    public void updateInfo(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public void addUserGroup(UserGroup userGroup) {
        userGroups.add(userGroup);
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateFocusNotificationInfo(Boolean isNotificationAgreed, Integer notificationCycle, String notificationMessage) {
        this.isNotificationAgreed = isNotificationAgreed;
        this.notificationCycle = notificationCycle;
        this.notificationMessage = notificationMessage;
    }

    public void updateGoalSeconds(Integer goalSeconds) {
        this.goalSeconds = goalSeconds;
    }

    public void updateLastNotificationSentAt(LocalDateTime lastNotificationSentAt) {
        this.lastNotificationSentAt = lastNotificationSentAt;
    }

    public void addAccumulatedDuration(Long seconds) {
        if (this.accumulatedDuration == null) {
            this.accumulatedDuration = 0L;
        }
        this.accumulatedDuration += seconds;
    }

    public void resetAccumulatedDuration() {
        this.accumulatedDuration = 0L;
    }
}