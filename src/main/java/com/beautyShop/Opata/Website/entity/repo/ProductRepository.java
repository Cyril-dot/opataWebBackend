package com.beautyShop.Opata.Website.entity.repo;

import com.beautyShop.Opata.Website.entity.Product;
import com.beautyShop.Opata.Website.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ── CATEGORY ─────────────────────────────────────────────
    List<Product> findByCategory(String category);
    List<Product> findByCategoryIgnoreCase(String category);

    // ── SUB-CATEGORY ─────────────────────────────────────────
    List<Product> findBySubCategory(SubCategory subCategory);

    // ── BRAND ────────────────────────────────────────────────
    List<Product> findByBrand(String brand);
    List<Product> findByBrandIgnoreCase(String brand);

    // ── NAME SEARCH ──────────────────────────────────────────
    List<Product> findByNameContainingIgnoreCase(String name);

    // ── NAME + CATEGORY ──────────────────────────────────────
    List<Product> findByNameContainingIgnoreCaseAndCategoryIgnoreCase(String name, String category);

    // ── NAME + BRAND ─────────────────────────────────────────
    List<Product> findByNameContainingIgnoreCaseAndBrandIgnoreCase(String name, String brand);

    // ── PRICE RANGE ──────────────────────────────────────────
    List<Product> findByPriceBetween(BigDecimal min, BigDecimal max);

    // ── CATEGORY + PRICE RANGE ───────────────────────────────
    List<Product> findByCategoryIgnoreCaseAndPriceBetween(String category, BigDecimal min, BigDecimal max);

    // ── IN STOCK ONLY ────────────────────────────────────────
    List<Product> findByStockGreaterThan(int stock);

    // ── AVAILABILITY ─────────────────────────────────────────
    List<Product> findByIsAvailableTrue();
    List<Product> findByIsAvailableFalse();

    // ── DISCOUNTED PRODUCTS ───────────────────────────────────
    List<Product> findByDiscountPercentageGreaterThan(BigDecimal zero);

    // ── SIZES ────────────────────────────────────────────────
    @Query("SELECT DISTINCT p FROM Product p JOIN p.availableSizes s WHERE s = :size")
    List<Product> findByAvailableSizesContaining(@Param("size") ClothingSize size);

    // ── COLORS ───────────────────────────────────────────────
    @Query("SELECT DISTINCT p FROM Product p JOIN p.availableColors c WHERE c = :color")
    List<Product> findByAvailableColorsContaining(@Param("color") ClothingColor color);

    // ── PRODUCTS BY ADMIN ────────────────────────────────────
    List<Product> findByAddedById(UUID adminId);

    // ── FULL KEYWORD SEARCH (name + description + brand) ─────
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    // ── ALL DISTINCT CATEGORIES ───────────────────────────────
    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findAllCategories();

    // ── ALL DISTINCT BRANDS ───────────────────────────────────
    @Query("SELECT DISTINCT p.brand FROM Product p ORDER BY p.brand")
    List<String> findAllBrands();

    // ── ALL DISTINCT MATERIALS ────────────────────────────────
    @Query("SELECT DISTINCT p.material FROM Product p WHERE p.material IS NOT NULL ORDER BY p.material")
    List<String> findAllMaterials();
}