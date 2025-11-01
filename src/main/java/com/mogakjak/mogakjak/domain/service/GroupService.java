package com.mogakjak.mogakjak.domain.service;

import com.mogakjak.mogakjak.controller.dto.my_page.MyGroupResponse;
import java.util.List;
import java.util.UUID;

public interface GroupService {

    List<MyGroupResponse> getMyGroups(UUID userId);
}