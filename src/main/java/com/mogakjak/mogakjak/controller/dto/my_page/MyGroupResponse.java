package com.mogakjak.mogakjak.controller.dto.my_page;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyGroupResponse {

    private UUID groupId;
    private String groupName;
}