package com.beautyShop.Opata.Website.entity.repo;


import com.beautyShop.Opata.Website.entity.Order;
import com.beautyShop.Opata.Website.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ── USER ─────────────────────────────────────────────────

    // User sees their own orders newest first
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Count total orders a user has placed
    long countByUserId(UUID userId);

    // ── ADMIN ────────────────────────────────────────────────

    // All orders newest first
    List<Order> findAllByOrderByCreatedAtDesc();

    // All orders highest value first
    List<Order> findAllByOrderByTotalAmountDesc();

    // Filter by status (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    // Orders placed within a date range (today, this week, this month, custom)
    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}