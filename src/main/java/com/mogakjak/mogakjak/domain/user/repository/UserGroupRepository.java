package com.mogakjak.mogakjak.domain.user.repository;

import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {

    // 내 그룹 목록 조회 (가입한 그룹 목록)
    @Query("SELECT ug FROM UserGroup ug JOIN FETCH ug.group WHERE ug.user = :user")
    List<UserGroup> findAllByUserWithGroup(@Param("user") User user);
}