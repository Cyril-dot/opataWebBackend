package com.beautyShop.Opata.Website.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "general_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Flexible category — can be anything: Electronics, Beauty, Food, Clothing, etc.
    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    // More specific classification within the category
    private String subCategory;

    private String brand;

    // SKU for inventory tracking
    @Column(unique = true)
    private String sku;

    @NotNull(message = "Stock is required")
    @Column(nullable = false)
    private Integer stock;

    @Builder.Default
    private Boolean isAvailable = true;

    // Unit of measurement e.g. "kg", "piece", "litre", "pack"
    private String unit;

    // Weight in kg — useful for shipping calculations
    private Double weightKg;

    // Dimensions — useful for shipping
    private Double lengthCm;
    private Double widthCm;
    private Double heightCm;

    // Discount percentage (e.g. 10 for 10% off)
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    // Tags for search/filtering e.g. "sale", "new arrival", "trending"
    @ElementCollection
    @CollectionTable(name = "general_product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    // Key-value pairs for flexible product attributes
    // e.g. {"Voltage": "220V", "Warranty": "1 year"} for electronics
    //      {"Scent": "Lavender", "Skin Type": "Oily"} for beauty
    @ElementCollection
    @CollectionTable(name = "general_product_attributes", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    @Builder.Default
    private java.util.Map<String, String> attributes = new java.util.HashMap<>();

    // ── Multiple images ─────────────────────────────────────
    @OneToMany(mappedBy = "generalProduct", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<GeneralProductImage> images = new ArrayList<>();

    // ── Linked to the shop owner who added this product ─────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_owner_id", nullable = false)
    private ShopOwner addedBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Convenience helpers ─────────────────────────────────

    /** Returns the primary (cover) image URL, or null if no images. */
    public String getPrimaryImageUrl() {
        return images.isEmpty() ? null : images.get(0).getImageUrl();
    }

    /** Returns the primary image public ID, or null if no images. */
    public String getPrimaryImagePublicId() {
        return images.isEmpty() ? null : images.get(0).getImagePublicId();
    }

    /** Returns the final price after discount, or original price if no discount. */
    public BigDecimal getFinalPrice() {
        if (discountPercentage == null || discountPercentage.compareTo(BigDecimal.ZERO) == 0) {
            return price;
        }
        BigDecimal discount = price.multiply(discountPercentage).divide(BigDecimal.valueOf(100));
        return price.subtract(discount);
    }

    /** Returns true if the product is in stock. */
    public boolean isInStock() {
        return stock != null && stock > 0;
    }
}