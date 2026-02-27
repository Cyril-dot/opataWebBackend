package com.beautyShop.Opata.Website.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "general_product_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    // Cloudinary public ID for deletion/transformation
    private String imagePublicId;

    // 0 = primary/cover image
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "general_product_id", nullable = false)
    private GeneralProduct generalProduct;
}