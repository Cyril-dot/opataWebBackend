package com.beautyShop.Opata.Website.entity.repo;

import com.beautyShop.Opata.Website.entity.ChatRoom;
import com.beautyShop.Opata.Website.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // ── Find by user + product (prevent duplicate product chats) ─
    Optional<ChatRoom> findByUserIdAndProductId(UUID userId, Long productId);

    // ── Find by user + order (prevent duplicate order chats) ─────
    Optional<ChatRoom> findByUserIdAndLinkedOrderId(UUID userId, Long orderId);

    // ── All rooms for a user ──────────────────────────────────────
    List<ChatRoom> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // ── Rooms by user + type ──────────────────────────────────────
    List<ChatRoom> findByUserIdAndRoomTypeOrderByCreatedAtDesc(UUID userId, ChatRoomType roomType);

    // ── All rooms for an admin ────────────────────────────────────
    List<ChatRoom> findByShopOwnerIdOrderByCreatedAtDesc(UUID adminId);

    Optional<ChatRoom> findByLinkedDeliveryId(Long deliveryId);

    // ── Rooms by admin + type ─────────────────────────────────────
    List<ChatRoom> findByShopOwnerIdAndRoomTypeOrderByCreatedAtDesc(UUID adminId, ChatRoomType roomType);
}