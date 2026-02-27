package com.beautyShop.Opata.Website.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// ── Used when admin creates or updates a general product ─────
@Data
public class GeneralProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    // e.g. Electronics, Beauty, Food, Home & Garden, Sports
    @NotBlank(message = "Category is required")
    private String category;

    // Free-text — flexible for any product type
    // e.g. "Smartphones", "Face Cream", "Protein Powder"
    private String subCategory;

    private String brand;

    // Unique stock-keeping unit — optional but recommended
    private String sku;

    @NotNull(message = "Stock is required")
    private Integer stock;

    // e.g. "piece", "kg", "litre", "pack", "box"
    private String unit;

    // Shipping info — optional
    private Double weightKg;
    private Double lengthCm;
    private Double widthCm;
    private Double heightCm;

    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    @DecimalMax(value = "100.0", message = "Discount cannot exceed 100%")
    private BigDecimal discountPercentage;

    // e.g. ["sale", "new arrival", "trending", "featured"]
    private List<String> tags;

    // Flexible key-value attributes for any product type
    // Electronics: {"Voltage": "220V", "Warranty": "1 year", "Color": "Black"}
    // Beauty:      {"Scent": "Lavender", "Skin Type": "Oily", "Volume": "200ml"}
    // Food:        {"Weight": "500g", "Allergens": "Nuts", "Expiry": "12 months"}
    private Map<String, String> attributes;

    private Boolean isAvailable = true;

    // Images handled separately as MultipartFile in the controller
}