package com.beautyShop.Opata.Website.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// ── Used when reordering or updating a specific image ────────
@Data
public class GeneralProductImageRequest {

    @NotNull(message = "Image ID is required")
    private Long imageId;

    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;
}