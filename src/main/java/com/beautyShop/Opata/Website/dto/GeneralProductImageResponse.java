package com.beautyShop.Opata.Website.dto;

import lombok.Builder;
import lombok.Data;

// ── Returned when viewing a single general product image ─────
@Data
@Builder
public class GeneralProductImageResponse {

    private Long id;
    private String imageUrl;
    private String imagePublicId;
    private Integer displayOrder;
    private Long generalProductId;
    private String generalProductName;
}