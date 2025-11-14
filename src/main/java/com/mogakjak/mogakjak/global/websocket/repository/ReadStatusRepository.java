package com.mogakjak.mogakjak.global.websocket.repository;

import com.mogakjak.mogakjak.domain.user.entity.User;
import com.mogakjak.mogakjak.global.websocket.domain.ChatRoom;
import com.mogakjak.mogakjak.global.websocket.domain.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReadStatusRepository extends JpaRepository<ReadStatus, UUID> {
    List<ReadStatus> findByChatRoomAndMember(ChatRoom chatRoom, User member);
    Long countByChatRoomAndMemberAndIsReadFalse(ChatRoom chatRoom, User member);
}
