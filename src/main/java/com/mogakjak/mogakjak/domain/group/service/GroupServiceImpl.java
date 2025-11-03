package com.mogakjak.mogakjak.domain.group.service;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.group.controller.dto.MyGroupResponse;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.repository.UserGroupRepository;
import com.mogakjak.mogakjak.domain.user.repository.UserRepository;
import com.mogakjak.mogakjak.global.exception.CustomException;
import com.mogakjak.mogakjak.global.exception.status.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupServiceImpl implements GroupService {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;

    // 내 그룹 목록 조회
    @Override
    public List<MyGroupResponse> getMyGroups(UUID userId) {
        User user = findUserById(userId);

        return userGroupRepository.findAllByUserWithGroup(user).stream()
                .map(userGroup -> toMyGroupDto(userGroup.getGroup()))
                .collect(Collectors.toList());
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private MyGroupResponse toMyGroupDto(Group group) {
        return MyGroupResponse.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .build();
    }
}