package com.beautyShop.Opata.Website.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ── Returned when viewing a general product ──────────────────
@Data
@Builder
public class GeneralProductResponse {

    private Long id;
    private String name;
    private String description;

    // Original price
    private BigDecimal price;

    // Price after discount is applied
    private BigDecimal finalPrice;

    // e.g. 10.00 means 10% off
    private BigDecimal discountPercentage;

    // e.g. Electronics, Beauty, Food
    private String category;

    // e.g. Smartphones, Face Cream, Protein Powder
    private String subCategory;

    private String brand;
    private String sku;
    private Integer stock;
    private Boolean isAvailable;

    // e.g. piece, kg, litre, pack
    private String unit;

    // Shipping dimensions & weight
    private Double weightKg;
    private Double lengthCm;
    private Double widthCm;
    private Double heightCm;

    // e.g. ["sale", "trending", "new arrival"]
    private List<String> tags;

    // e.g. {"Voltage": "220V", "Warranty": "1 year"}
    private Map<String, String> attributes;

    // Primary (cover) image URL
    private String imageUrl;

    // All images in display order
    private List<String> imageUrls;

    // Name of the shop owner who added this product
    private String addedByAdmin;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}