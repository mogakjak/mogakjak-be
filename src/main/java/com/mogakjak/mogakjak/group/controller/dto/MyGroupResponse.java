package com.mogakjak.mogakjak.group.controller.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyGroupResponse {

    private UUID groupId;
    private String groupName;
}