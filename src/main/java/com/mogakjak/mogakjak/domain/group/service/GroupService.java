package com.mogakjak.mogakjak.domain.group.service;

import com.mogakjak.mogakjak.domain.group.controller.dto.MyGroupResponse;
import java.util.List;
import java.util.UUID;

public interface GroupService {

    List<MyGroupResponse> getMyGroups(UUID userId);
}