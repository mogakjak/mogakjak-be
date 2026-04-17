package com.mogakjak.mogakjak.domain.user.repository;

import com.mogakjak.mogakjak.domain.group.entity.Group;
import com.mogakjak.mogakjak.domain.user.entity.GroupParticipationStatus;
import com.mogakjak.mogakjak.domain.user.entity.GroupRole;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserGroup;
import com.mogakjak.mogakjak.domain.user.repository.projection.SharedGroupNameProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {

    // 내 그룹 목록 조회 (가입한 그룹 목록 + 그룹 정보 Fetch Join)
    @Query("SELECT ug FROM UserGroup ug JOIN FETCH ug.group " +
            "WHERE ug.user = :user AND ug.user.isDeleted = false")
    List<UserGroup> findAllByUserWithGroup(@Param("user") User user);

    // 특정 유저와 그룹으로 UserGroup 엔티티 조회 (객체 파라미터)
    Optional<UserGroup> findByUserAndGroup(User user, Group group);

    // 특정 유저 ID와 그룹 ID로 UserGroup 엔티티 조회 (ID 파라미터)
    Optional<UserGroup> findByUser_IdAndGroup_Id(UUID userId, UUID groupId);

    // 특정 그룹에 속한 모든 UserGroup 조회 (멤버 조회용, 탈퇴 사용자 제외)
    @Query("SELECT ug FROM UserGroup ug JOIN FETCH ug.user " +
            "WHERE ug.group = :group AND ug.user.isDeleted = false")
    List<UserGroup> findAllByGroupWithUser(@Param("group") Group group);

    // 그룹 상세 조회 시 멤버와 프로필 정보를 함께 가져오기 위한 쿼리 (탈퇴 사용자 제외)
    @Query("SELECT ug FROM UserGroup ug " +
            "JOIN FETCH ug.user u " +
            "LEFT JOIN FETCH u.userProfile up " +
            "LEFT JOIN FETCH up.mainImageCharacter " +
            "WHERE ug.group.id = :groupId AND u.isDeleted = false")
    List<UserGroup> findByGroupIdWithUserAndProfile(@Param("groupId") UUID groupId);

    // 유저가 그룹의 호스트인지 확인
    boolean existsByUserAndGroupAndRole(User user, Group group, GroupRole role);

    // 내 전체 메이트 조회 (User 객체 반환, 탈퇴 사용자 제외)
    @Query("SELECT DISTINCT ug.user FROM UserGroup ug " +
            "WHERE ug.group IN (SELECT ug_sub.group FROM UserGroup ug_sub WHERE ug_sub.user = :user) " +
            "AND ug.user != :user " +
            "AND ug.user.isDeleted = false " +
            "AND (:search IS NULL OR ug.user.name LIKE %:search%)")
    Page<User> findTotalMatesByUser(
            @Param("user") User user,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT ug.group.name FROM UserGroup ug " +
            "WHERE ug.user = :mate " +
            "AND ug.group IN (SELECT myUg.group FROM UserGroup myUg WHERE myUg.user = :me)")
    List<String> findSharedGroupNames(@Param("me") User me, @Param("mate") User mate);

    @Query("""
            SELECT DISTINCT ug.user.id AS mateId, ug.group.name AS groupName
            FROM UserGroup ug
            WHERE ug.user.id IN :mateIds
              AND ug.group IN (SELECT myUg.group FROM UserGroup myUg WHERE myUg.user = :me)
              AND ug.user != :me
              AND ug.user.isDeleted = false
            ORDER BY ug.user.id ASC
            """)
    List<SharedGroupNameProjection> findSharedGroupNamesByMates(
            @Param("me") User me,
            @Param("mateIds") Collection<UUID> mateIds
    );

    // 내 전체 메이트 조회 (UserGroup 객체 반환 - 그룹명 포함용, 탈퇴 사용자 제외)
    @Query("SELECT ug FROM UserGroup ug " +
            "JOIN FETCH ug.user " +
            "JOIN FETCH ug.group " + // 그룹 정보도 함께 Fetch
            "WHERE ug.group IN (SELECT sub.group FROM UserGroup sub WHERE sub.user = :user) " +
            "AND ug.user != :user " +
            "AND ug.user.isDeleted = false " +
            "AND (:search IS NULL OR ug.user.name LIKE %:search%)")
    Page<UserGroup> findTotalMatesWithGroupByUser(
            @Param("user") User user,
            @Param("search") String search,
            Pageable pageable
    );

    // 특정 그룹 내 메이트 조회 (탈퇴 사용자 제외)
    @Query("SELECT ug.user FROM UserGroup ug " +
            "WHERE ug.group = :group " +
            "AND ug.user != :user " +
            "AND ug.user.isDeleted = false " +
            "AND (:search IS NULL OR ug.user.name LIKE %:search%)")
    Page<User> findMatesByGroup(
            @Param("group") Group group,
            @Param("user") User user,
            @Param("search") String search,
            Pageable pageable
    );

    // 그룹 멤버 수 (탈퇴 사용자 제외)
    @Query("SELECT COUNT(ug) FROM UserGroup ug " +
            "WHERE ug.group = :group AND ug.user.isDeleted = false")
    long countByGroup(@Param("group") Group group);

    // 그룹 내 활동(참여) 중인 멤버 수 (탈퇴 사용자 제외, NOT_PARTICIPATING 제외)
    @Query("SELECT COUNT(ug) FROM UserGroup ug " +
            "WHERE ug.group = :group " +
            "AND ug.user.isDeleted = false " +
            "AND ug.participationStatus <> :notParticipating")
    long countActiveByGroup(
            @Param("group") Group group,
            @Param("notParticipating") GroupParticipationStatus notParticipating
    );
    
    // 두 사용자가 함께 있는 그룹 목록 조회
    @Query("SELECT ug1.group FROM UserGroup ug1 " +
            "WHERE ug1.user.id = :userId1 " +
            "AND ug1.group IN (SELECT ug2.group FROM UserGroup ug2 WHERE ug2.user.id = :userId2) " +
            "ORDER BY ug1.group.createdAt DESC")
    List<Group> findCommonGroups(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    Optional<UserGroup> findTopByGroupAndUserNotOrderByCreatedAtAsc(Group group, User user);

    // 라운지에 있는 멤버들 중에서, 나와 같은 그룹에 속한 메이트들의 ID를 조회
    @Query("""
          SELECT DISTINCT ug.user.id
          FROM UserGroup ug
          WHERE ug.user.id IN :memberIds
            AND ug.group IN (SELECT myUg.group FROM UserGroup myUg WHERE myUg.user.id = :myUserId)
            AND ug.user.id != :myUserId
            AND ug.user.isDeleted = false
          """)
    Set<UUID> findMateIdsByUser(
            @Param("myUserId") UUID myUserId,
            @Param("memberIds") Collection<UUID> memberIds
    );
}
