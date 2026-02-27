package com.beautyShop.Opata.Website.dto;

import com.beautyShop.Opata.Website.entity.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// ── Used when admin creates or updates a product ─────────────
@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    // e.g. Men, Women, Kids, Unisex
    @NotBlank(message = "Category is required")
    private String category;

    // e.g. DRESS, T_SHIRT, JEANS — from SubCategory enum
    @NotNull(message = "Sub-category is required")
    private SubCategory subCategory;

    @NotBlank(message = "Brand is required")
    private String brand;

    // e.g. [S, M, L, XL]
    private List<ClothingSize> availableSizes;

    // e.g. [RED, BLACK, WHITE]
    private List<ClothingColor> availableColors;

    // e.g. Cotton, Polyester, Linen
    private String material;

    // e.g. Casual, Formal, Streetwear
    private String style;

    @NotNull(message = "Stock is required")
    private Integer stock;

    // e.g. 10.00 means 10% off — optional
    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    @DecimalMax(value = "100.0", message = "Discount cannot exceed 100%")
    private BigDecimal discountPercentage;

    private Boolean isAvailable = true;

    // Images are handled separately as MultipartFile in the controller
}