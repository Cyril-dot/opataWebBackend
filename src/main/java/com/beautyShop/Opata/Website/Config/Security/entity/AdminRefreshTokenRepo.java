package com.beautyShop.Opata.Website.Config.Security.entity;

import com.beautyShop.Opata.Website.entity.ShopOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AdminRefreshTokenRepo extends JpaRepository<AdminRefreshToken, Long> {
    /**
     * Find refresh token by the token string itself
     * ✅ CRITICAL: This was missing and is needed for token validation
     */
    Optional<AdminRefreshToken> findByToken(String token);

    /**
     * Find refresh token by User entity
     */
    Optional<AdminRefreshToken> findByOwner(ShopOwner owner);

    /**
     * Upsert refresh token for a user (PostgreSQL syntax)
     * Inserts new token or updates existing one for the user
     */
    @Modifying
    @Transactional
    @Query(
            value = """
            INSERT INTO admin_tokens (owner_id, token, expiry_date)
            VALUES (:ownerId, :token, :expiry)
            ON CONFLICT (owner_id)
            DO UPDATE SET
                token = EXCLUDED.token,
                expiry_date = EXCLUDED.expiry_date
        """,
            nativeQuery = true
    )
    void upsertOwnerRefreshToken(@Param("ownerId") UUID ownerId,
                                 @Param("token") String token,
                                 @Param("expiry") Instant expiryDate);  // ← "expiry" not "expiryDate"
}
