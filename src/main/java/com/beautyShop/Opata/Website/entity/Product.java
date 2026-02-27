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
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

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

    // e.g. Men, Women, Kids, Unisex
    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;

    // e.g. Tops, Dresses, Trousers, Jackets
    @NotNull(message = "Sub-category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubCategory subCategory;

    @NotBlank(message = "Brand is required")
    @Column(nullable = false)
    private String brand;

    // Available sizes using enum
    @ElementCollection(targetClass = ClothingSize.class)
    @CollectionTable(name = "product_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "size")
    @Builder.Default
    private List<ClothingSize> availableSizes = new ArrayList<>();

    // Available colors using enum
    @ElementCollection(targetClass = ClothingColor.class)
    @CollectionTable(name = "product_colors", joinColumns = @JoinColumn(name = "product_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "color")
    @Builder.Default
    private List<ClothingColor> availableColors = new ArrayList<>();

    // e.g. Cotton, Polyester, Linen
    private String material;

    // e.g. Casual, Formal, Sportswear
    private String style;

    @NotNull(message = "Stock is required")
    @Column(nullable = false)
    private Integer stock;

    @Builder.Default
    private Boolean isAvailable = true;

    // Discount percentage (e.g. 10 for 10% off)
    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    // ── Multiple images ─────────────────────────────────────
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

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
}