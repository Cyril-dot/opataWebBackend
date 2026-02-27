package com.beautyShop.Opata.Website.entity.repo;

import com.beautyShop.Opata.Website.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Get all items belonging to a specific order
    List<OrderItem> findByOrderId(Long orderId);

    // Get all order items for a specific product
    // (admin: see how many times a product was ordered)
    List<OrderItem> findByProductId(Long productId);

    // Get all order items for a specific user across all their orders
    // (navigates Order → User because OrderItem has no direct userId field)
    List<OrderItem> findByOrderUserId(UUID userId);

    // Check if a specific product exists anywhere in a user's order history
    // (same navigation: OrderItem → Order → User)
    Optional<OrderItem> findByOrderUserIdAndProductId(Long userId, Long productId);

    // Check if a product already exists inside a specific order
    // (useful to avoid duplicate line items in the same order)
    Optional<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);

    // Count how many times a specific product has been ordered
    long countByProductId(Long productId);

    // Total quantity sold for a specific product (stock analytics)
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.product.id = :productId")
    int getTotalQuantitySoldByProduct(@Param("productId") Long productId);

    // Admin: rank products by total units sold (best sellers)
    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) as totalSold " +
            "FROM OrderItem oi GROUP BY oi.product.id, oi.product.name ORDER BY totalSold DESC")
    List<Object[]> findBestSellingProducts();
}