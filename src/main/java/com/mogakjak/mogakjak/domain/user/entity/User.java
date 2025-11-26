package com.mogakjak.mogakjak.domain.user.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseSchema {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String imageUrl;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private UserProfile userProfile;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @Column(nullable = true)
    private LocalDateTime lastActivityAt;

    public void updateInfo(String newName, String newEmail, String newImageUrl) {
        this.name = newName;
        this.email = newEmail;
        this.imageUrl = newImageUrl;
    }

    public void setActive(Boolean isActive) {
        this.isActive = isActive;
        // 활동 상태가 true로 변경될 때만 lastActivityAt 업데이트
        if (isActive) {
            this.lastActivityAt = LocalDateTime.now();
        }
    }
}