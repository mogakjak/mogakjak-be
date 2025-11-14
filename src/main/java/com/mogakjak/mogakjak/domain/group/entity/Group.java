package com.mogakjak.mogakjak.domain.group.entity;

import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.global.common.BaseSchema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

    private String description;

    private String password;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserGroup> userGroups = new ArrayList<>();

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
}