package com.mogakjak.mogakjak.group.service;

import com.mogakjak.mogakjak.group.controller.dto.MyGroupResponse;
import java.util.List;
import java.util.UUID;

public interface GroupService {

    List<MyGroupResponse> getMyGroups(UUID userId);
}