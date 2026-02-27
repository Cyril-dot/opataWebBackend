package com.beautyShop.Opata.Website.entity.repo;

import com.beautyShop.Opata.Website.entity.GeneralProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface GeneralProductRepository extends JpaRepository<GeneralProduct, Long> {

    // ── NAME SEARCH ──────────────────────────────────────────
    List<GeneralProduct> findByNameContainingIgnoreCase(String name);

    // ── CATEGORY ─────────────────────────────────────────────
    List<GeneralProduct> findByCategory(String category);
    List<GeneralProduct> findByCategoryIgnoreCase(String category);

    // ── SUB-CATEGORY ─────────────────────────────────────────
    List<GeneralProduct> findBySubCategoryIgnoreCase(String subCategory);

    // ── BRAND ────────────────────────────────────────────────
    List<GeneralProduct> findByBrand(String brand);
    List<GeneralProduct> findByBrandIgnoreCase(String brand);

    // ── SKU ──────────────────────────────────────────────────
    java.util.Optional<GeneralProduct> findBySku(String sku);

    // ── AVAILABILITY ─────────────────────────────────────────
    List<GeneralProduct> findByIsAvailableTrue();
    List<GeneralProduct> findByIsAvailableFalse();

    // ── STOCK ────────────────────────────────────────────────
    List<GeneralProduct> findByStockGreaterThan(int stock);
    List<GeneralProduct> findByStockLessThanEqual(int threshold);  // low stock alert

    // ── PRICE RANGE ──────────────────────────────────────────
    List<GeneralProduct> findByPriceBetween(BigDecimal min, BigDecimal max);

    // ── DISCOUNTED PRODUCTS ───────────────────────────────────
    List<GeneralProduct> findByDiscountPercentageGreaterThan(BigDecimal zero);

    // ── CATEGORY + PRICE RANGE ───────────────────────────────
    List<GeneralProduct> findByCategoryIgnoreCaseAndPriceBetween(String category, BigDecimal min, BigDecimal max);

    // ── PRODUCTS BY ADMIN ─────────────────────────────────────
    List<GeneralProduct> findByAddedById(UUID adminId);

    // ── TAGS — uses JOIN because tags is an @ElementCollection ─
    @Query("SELECT DISTINCT p FROM GeneralProduct p JOIN p.tags t WHERE LOWER(t) = LOWER(:tag)")
    List<GeneralProduct> findByTagsContaining(@Param("tag") String tag);

    // ── KEYWORD SEARCH (name + description + brand + category) ─
    @Query("SELECT p FROM GeneralProduct p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.subCategory) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<GeneralProduct> searchByKeyword(@Param("keyword") String keyword);

    // ── ATTRIBUTE SEARCH — find by a specific attribute key-value pair ─
    @Query("SELECT DISTINCT p FROM GeneralProduct p JOIN p.attributes a " +
           "WHERE KEY(a) = :attrKey AND LOWER(VALUE(a)) = LOWER(:attrValue)")
    List<GeneralProduct> findByAttribute(@Param("attrKey") String attrKey,
                                          @Param("attrValue") String attrValue);

    // ── DISTINCT DROPDOWNS ────────────────────────────────────
    @Query("SELECT DISTINCT p.category FROM GeneralProduct p ORDER BY p.category")
    List<String> findAllCategories();

    @Query("SELECT DISTINCT p.subCategory FROM GeneralProduct p WHERE p.subCategory IS NOT NULL ORDER BY p.subCategory")
    List<String> findAllSubCategories();

    @Query("SELECT DISTINCT p.brand FROM GeneralProduct p WHERE p.brand IS NOT NULL ORDER BY p.brand")
    List<String> findAllBrands();

    @Query("SELECT DISTINCT t FROM GeneralProduct p JOIN p.tags t ORDER BY t")
    List<String> findAllTags();
}