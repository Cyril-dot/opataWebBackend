package com.beautyShop.Opata.Website.entity.repo;

import com.beautyShop.Opata.Website.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Get all messages in a chat room ordered by time
    List<ChatMessage> findByChatRoomIdOrderBySentAtAsc(Long chatRoomId);

    // Count messages in a room (useful for unread badges)
    long countByChatRoomId(Long chatRoomId);
}