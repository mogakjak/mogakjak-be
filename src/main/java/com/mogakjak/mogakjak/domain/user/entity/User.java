package com.mogakjak.mogakjak.domain.user.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import lombok.*;

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

    public void updateInfo(String newName, String newEmail, String newImageUrl) {
        this.name = newName;
        this.email = newEmail;
        this.imageUrl = newImageUrl;
    }
}