package com.mogakjak.mogakjak.domain.user.repository;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.user.entity.GroupRole;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {

    // 내 그룹 목록 조회 (가입한 그룹 목록 + 그룹 정보 Fetch Join)
    @Query("SELECT ug FROM UserGroup ug JOIN FETCH ug.group WHERE ug.user = :user")
    List<UserGroup> findAllByUserWithGroup(@Param("user") User user);

    // 특정 유저와 그룹으로 UserGroup 엔티티 조회 (객체 파라미터)
    Optional<UserGroup> findByUserAndGroup(User user, Group group);

    // 특정 유저 ID와 그룹 ID로 UserGroup 엔티티 조회 (ID 파라미터)
    Optional<UserGroup> findByUser_IdAndGroup_Id(UUID userId, UUID groupId);

    // 특정 그룹에 속한 모든 UserGroup 조회 (멤버 조회용)
    @Query("SELECT ug FROM UserGroup ug JOIN FETCH ug.user WHERE ug.group = :group")
    List<UserGroup> findAllByGroupWithUser(@Param("group") Group group);

    // 그룹 상세 조회 시 멤버와 프로필 정보를 함께 가져오기 위한 쿼리
    @Query("SELECT ug FROM UserGroup ug " +
            "JOIN FETCH ug.user u " +
            "LEFT JOIN FETCH u.userProfile up " +
            "LEFT JOIN FETCH up.mainImageCharacter " +
            "WHERE ug.group.id = :groupId")
    List<UserGroup> findByGroupIdWithUserAndProfile(@Param("groupId") UUID groupId);

    // 유저가 그룹의 호스트인지 확인
    boolean existsByUserAndGroupAndRole(User user, Group group, GroupRole role);

    // 내 전체 메이트 조회 (User 객체 반환)
    @Query("SELECT DISTINCT ug.user FROM UserGroup ug " +
            "WHERE ug.group IN (SELECT ug_sub.group FROM UserGroup ug_sub WHERE ug_sub.user = :user) " +
            "AND ug.user != :user " +
            "AND (:search IS NULL OR ug.user.name LIKE %:search%)")
    Page<User> findTotalMatesByUser(
            @Param("user") User user,
            @Param("search") String search,
            Pageable pageable
    );

    // 내 전체 메이트 조회 (UserGroup 객체 반환 - 그룹명 포함용)
    @Query("SELECT ug FROM UserGroup ug " +
            "JOIN FETCH ug.user " +
            "JOIN FETCH ug.group " + // 그룹 정보도 함께 Fetch
            "WHERE ug.group IN (SELECT sub.group FROM UserGroup sub WHERE sub.user = :user) " +
            "AND ug.user != :user " +
            "AND (:search IS NULL OR ug.user.name LIKE %:search%)")
    Page<UserGroup> findTotalMatesWithGroupByUser(
            @Param("user") User user,
            @Param("search") String search,
            Pageable pageable
    );

    // 특정 그룹 내 메이트 조회
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