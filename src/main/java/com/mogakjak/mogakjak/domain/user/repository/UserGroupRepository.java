package com.mogakjak.mogakjak.domain.user.repository;

import com.mogakjak.mogakjak.domain.user.entity.GroupRole;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.group.entity.Group;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {

    // 내 그룹 목록 조회 (가입한 그룹 목록)
    @Query("SELECT ug FROM UserGroup ug JOIN FETCH ug.group WHERE ug.user = :user")
    List<UserGroup> findAllByUserWithGroup(@Param("user") User user);

    // 특정 유저와 그룹으로 UserGroup 엔티티 조회
    Optional<UserGroup> findByUserAndGroup(User user, Group group);
    Optional<UserGroup> findByUser_IdAndGroup_Id(UUID userId, UUID groupId);

    // 특정 그룹에 속한 모든 UserGroup 조회 (멤버 조회용)
    @Query("SELECT ug FROM UserGroup ug JOIN FETCH ug.user WHERE ug.group = :group")
    List<UserGroup> findAllByGroupWithUser(@Param("group") Group group);

    // 유저가 그룹의 호스트인지 확인
    boolean existsByUserAndGroupAndRole(User user, Group group, GroupRole role);

    @Query("SELECT DISTINCT ug.user FROM UserGroup ug " +
            "WHERE ug.group IN (SELECT ug_sub.group FROM UserGroup ug_sub WHERE ug_sub.user = :user) " +
            "AND ug.user != :user " +
            "AND (:search IS NULL OR ug.user.name LIKE %:search%)")
    Page<User> findTotalMatesByUser(
            @Param("user") User user,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT ug.user FROM UserGroup ug " +
            "WHERE ug.group = :group " +
            "AND ug.user != :user " +
            "AND (:search IS NULL OR ug.user.name LIKE %:search%)")
    Page<User> findMatesByGroup(
            @Param("group") Group group,
            @Param("user") User user,
            @Param("search") String search,
            Pageable pageable
    );

    long countByGroup(Group group);
}