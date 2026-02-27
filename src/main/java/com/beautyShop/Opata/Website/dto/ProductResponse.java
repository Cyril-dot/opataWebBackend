package com.beautyShop.Opata.Website.dto;

import com.beautyShop.Opata.Website.entity.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ── Returned when viewing products ───────────────────────────
@Data
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;

    // Original price
    private BigDecimal price;

    // Price after discount is applied
    private BigDecimal finalPrice;

    // e.g. 10.00 means 10% off
    private BigDecimal discountPercentage;

    // e.g. Men, Women, Kids, Unisex
    private String category;

    // e.g. DRESS, T_SHIRT, JEANS
    private SubCategory subCategory;

    private String brand;

    // e.g. [S, M, L, XL]
    private List<ClothingSize> availableSizes;

    // e.g. [RED, BLACK, WHITE]
    private List<ClothingColor> availableColors;

    // e.g. Cotton, Polyester
    private String material;

    // e.g. Casual, Formal
    private String style;

    private Integer stock;
    private Boolean isAvailable;

    // Primary (cover) image URL
    private String imageUrl;

    // All images in display order
    private List<String> imageUrls;

    // Name of the shop owner who added this product
    private String addedByAdmin;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}