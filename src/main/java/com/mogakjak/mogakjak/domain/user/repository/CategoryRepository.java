package com.mogakjak.mogakjak.domain.user.repository;

import com.mogakjak.mogakjak.domain.user.entity.Category;
import com.mogakjak.mogakjak.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // 유저의 모든 카테고리를 순서(displayOrder)대로 조회
    List<Category> findAllByUserOrderByDisplayOrderAsc(User user);
    List<Category> findAllByUserAndIsDeletedFalseOrderByDisplayOrderAsc(User user);

    // 유저가 소유한 모든 카테고리 조회
    List<Category> findAllByUser(User user);
    List<Category> findAllByUserAndIsDeletedFalse(User user);

    // 특정 카테고리 ID와 유저로 조회 (소유권 검증)
    Optional<Category> findByIdAndUser(UUID id, User user);
    Optional<Category> findByIdAndUserAndIsDeletedFalse(UUID id, User user);

    // 유저의 카테고리 중 가장 큰 displayOrder 값을 조회
    @Query("""
        SELECT MAX(c.displayOrder)
        FROM Category c
        WHERE c.user = :user
        AND c.isDeleted = false
    """)
    Optional<Integer> findMaxDisplayOrderByUser(@Param("user") User user);

    @Query(value = """
    SELECT
        c.id AS categoryId,
        c.name AS categoryName,
        c.color AS color,
        COALESCE(SUM(s.total_duration), 0) AS totalSeconds,
        
        /* 완료된 Todo 개수 */
        (SELECT COUNT(*)
         FROM todo t
         WHERE t.category_id = c.id
           AND t.is_completed = 1
           AND t.is_deleted = 0
           AND t.date BETWEEN DATE(:start) AND DATE(:end)
        ) AS completedTodoCount,
        
        /* 전체 Todo 개수 */
        (SELECT COUNT(*)
         FROM todo t
         WHERE t.category_id = c.id
           AND t.is_deleted = 0
           AND t.date BETWEEN DATE(:start) AND DATE(:end)
        ) AS totalTodoCount

    FROM category c
    LEFT JOIN focus_session s
        ON s.category_id = c.id
       AND s.user_id = :userId
       AND s.started_at BETWEEN :start AND :end

    WHERE c.user_id = :userId
      AND c.is_deleted = 0

    GROUP BY c.id, c.name, c.color
    ORDER BY totalSeconds DESC
    """,
        nativeQuery = true)
    List<Map<String, Object>> getRawCategoryStats(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}