package com.beautyShop.Opata.Website.entity.repo;

import com.beautyShop.Opata.Website.entity.GeneralProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GeneralProductImageRepository extends JpaRepository<GeneralProductImage, Long> {

    // ── GET ALL IMAGES FOR A PRODUCT ─────────────────────────
    List<GeneralProductImage> findByGeneralProductId(Long generalProductId);

    // ── GET IMAGES IN DISPLAY ORDER ───────────────────────────
    List<GeneralProductImage> findByGeneralProductIdOrderByDisplayOrderAsc(Long generalProductId);

    // ── GET PRIMARY (COVER) IMAGE ─────────────────────────────
    java.util.Optional<GeneralProductImage> findFirstByGeneralProductIdOrderByDisplayOrderAsc(Long generalProductId);

    // ── COUNT IMAGES FOR A PRODUCT ────────────────────────────
    long countByGeneralProductId(Long generalProductId);

    // ── DELETE ALL IMAGES FOR A PRODUCT ──────────────────────
    @Modifying
    @Transactional
    void deleteByGeneralProductId(Long generalProductId);

    // ── CHECK IF PUBLIC ID EXISTS (avoid duplicate uploads) ───
    boolean existsByImagePublicId(String imagePublicId);

    // ── GET BY CLOUDINARY PUBLIC ID ───────────────────────────
    java.util.Optional<GeneralProductImage> findByImagePublicId(String imagePublicId);

    // ── GET ALL IMAGES ACROSS ALL PRODUCTS (admin cleanup use) ─
    @Query("SELECT i FROM GeneralProductImage i ORDER BY i.generalProduct.id ASC, i.displayOrder ASC")
    List<GeneralProductImage> findAllOrderedByProductAndDisplay();

    // ── UPDATE DISPLAY ORDER ──────────────────────────────────
    @Modifying
    @Transactional
    @Query("UPDATE GeneralProductImage i SET i.displayOrder = :newOrder WHERE i.id = :imageId")
    void updateDisplayOrder(@Param("imageId") Long imageId, @Param("newOrder") int newOrder);
}