package com.beautyShop.Opata.Website.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

// ── Used when deleting or reordering a batch of images ───────
@Data
public class ImageBatchRequest {

    // IDs of images to delete
    @NotEmpty(message = "At least one image ID is required")
    private List<Long> imageIds;
}