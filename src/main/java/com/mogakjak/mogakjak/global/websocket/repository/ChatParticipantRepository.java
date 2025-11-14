package com.mogakjak.mogakjak.global.websocket.repository;

import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.websocket.domain.ChatParticipant;
import com.mogakjak.mogakjak.global.websocket.domain.ChatRoom;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, UUID> {
    List<ChatParticipant> findByChatRoom(ChatRoom chatRoom);
    Optional<ChatParticipant> findByChatRoomAndMember(ChatRoom chatRoom, User member);
    List<ChatParticipant> findAllByMember(User member);

    @Query("SELECT cp1.chatRoom FROM ChatParticipant cp1 JOIN ChatParticipant cp2 ON cp1.chatRoom.id = cp2.chatRoom.id WHERE cp1.member.id = :myId AND cp2.member.id = :otherMemberId AND cp1.chatRoom.isGroupChat = 'N'")
    Optional<ChatRoom> findExistingPrivateRoom(@Param("myId") UUID myId, @Param("otherMemberId") UUID otherMemberId);
}
