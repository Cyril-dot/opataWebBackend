package com.beautyShop.Opata.Website.entity.repo;

import com.beautyShop.Opata.Website.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Get all cart items for a user (their full cart)
    List<CartItem> findByUserId(UUID userId);

    // Check if a product is already in the user's cart
    // (used to update quantity instead of creating a duplicate)
    Optional<CartItem> findByUserIdAndProductId(UUID userId, Long productId);

    // Count how many distinct items are in a user's cart
    long countByUserId(UUID userId);

    // Clear entire cart after order is placed
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}