package com.mogakjak.mogakjak.domain.user.repository;

import com.mogakjak.mogakjak.domain.user.entity.ImageCharacter;
import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.domain.user.entity.UserCharacter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, UUID> {

    @Query("SELECT uc FROM UserCharacter uc JOIN FETCH uc.imageCharacter WHERE uc.user = :user")
    List<UserCharacter> findAllByUser(@Param("user") User user);

    @Query("""
            SELECT uc
            FROM UserCharacter uc
            JOIN FETCH uc.user
            JOIN FETCH uc.imageCharacter
            WHERE uc.user.id IN :userIds
            ORDER BY uc.user.id ASC, uc.imageCharacter.level DESC, uc.imageCharacter.createdAt ASC
            """)
    List<UserCharacter> findAllByUserIdInOrderByUserIdAscImageCharacter_LevelDescImageCharacter_CreatedAtAsc(
            @Param("userIds") Collection<UUID> userIds
    );

    boolean existsByUserAndImageCharacter(User user, ImageCharacter imageCharacter);

    Optional<UserCharacter> findTopByUserOrderByImageCharacter_LevelDescImageCharacter_CreatedAtAsc(User user);
}
