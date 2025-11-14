package com.mogakjak.mogakjak.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GroupRole {
    HOST("방장"),
    MEMBER("멤버");

    private final String description;
}