package com.mogakjak.mogakjak.global.websocket.repository;

import com.mogakjak.mogakjak.global.websocket.domain.ChatMessage;
import com.mogakjak.mogakjak.global.websocket.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);
}
