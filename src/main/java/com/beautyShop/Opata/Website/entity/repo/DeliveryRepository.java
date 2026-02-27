package com.beautyShop.Opata.Website.entity.repo;

import com.beautyShop.Opata.Website.entity.Delivery;
import com.beautyShop.Opata.Website.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // ── BY USER ──────────────────────────────────────────────
    List<Delivery> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Delivery> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, DeliveryStatus status);

    // ── BY ORDER ─────────────────────────────────────────────
    Optional<Delivery> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);

    // ── BY STATUS ────────────────────────────────────────────
    List<Delivery> findByStatusOrderByCreatedAtDesc(DeliveryStatus status);

    // ── BY ADMIN ─────────────────────────────────────────────
    List<Delivery> findByAssignedAdminIdOrderByCreatedAtDesc(UUID adminId);
    List<Delivery> findByAssignedAdminIdAndStatusOrderByCreatedAtDesc(UUID adminId, DeliveryStatus status);

    // ── BY DATE RANGE ────────────────────────────────────────
    List<Delivery> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    // ── BY TRACKING NUMBER ───────────────────────────────────
    Optional<Delivery> findByTrackingNumber(String trackingNumber);

    // ── BY CITY ──────────────────────────────────────────────
    List<Delivery> findByCityIgnoreCase(String city);

    // ── COUNT ────────────────────────────────────────────────
    long countByUserId(UUID userId);
    long countByStatus(DeliveryStatus status);

    // ── ACTIVE DELIVERIES (not delivered or cancelled) ───────
    @Query("SELECT d FROM Delivery d WHERE d.status NOT IN " +
           "('DELIVERED', 'CANCELLED', 'FAILED') ORDER BY d.createdAt DESC")
    List<Delivery> findActiveDeliveries();

    // ── OVERDUE (past estimated time, not yet delivered) ─────
    @Query("SELECT d FROM Delivery d WHERE d.estimatedDeliveryTime < :now " +
           "AND d.status NOT IN ('DELIVERED', 'CANCELLED', 'FAILED')")
    List<Delivery> findOverdueDeliveries(@Param("now") LocalDateTime now);

}