package com.mogakjak.mogakjak.domain.user.entity;

import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseSchema {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    public void updateProfile(String newName) {
        this.name = newName;
    }
}