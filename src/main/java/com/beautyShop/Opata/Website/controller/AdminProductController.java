package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.Config.Security.AdminPrincipal;
import com.beautyShop.Opata.Website.dto.GeneralProductRequest;
import com.beautyShop.Opata.Website.dto.GeneralProductResponse;
import com.beautyShop.Opata.Website.dto.ProductRequest;
import com.beautyShop.Opata.Website.dto.ProductResponse;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.entity.ClothingColor;
import com.beautyShop.Opata.Website.entity.ClothingSize;
import com.beautyShop.Opata.Website.entity.SubCategory;
import com.beautyShop.Opata.Website.service.AdminProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Products", description = "Admin endpoints for managing clothing and general products")
public class AdminProductController {

    private final AdminProductService adminProductService;

    private AdminPrincipal adminPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new RuntimeException("User not authenticated");
        Object principal = auth.getPrincipal();
        if (!(principal instanceof AdminPrincipal)) throw new RuntimeException("Invalid authentication principal");
        return (AdminPrincipal) principal;
    }


    // ═══════════════════════════════════════════════════════════
    // CLOTHING PRODUCTS — WRITE
    // ═══════════════════════════════════════════════════════════

    @PostMapping(value = "/clothing", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Add a new clothing product",
        description = "Creates a clothing product, uploads images to Cloudinary, emails all users, and posts to Telegram channel"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Clothing product created"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Admin not found")
    })
    public ResponseEntity<ApiResult<ProductResponse>> addProduct(
            @RequestPart("product") ProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {

        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();

        log.info("➕ [ADMIN] Adding clothing product: {}", request.getName());
        ProductResponse response = adminProductService.addProduct(request, images, adminId);
        return ResponseEntity.ok(ApiResult.success("Clothing product added successfully", response));
    }

    @PutMapping(value = "/clothing/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a clothing product", description = "Updates product details and optionally adds/removes images")
    public ResponseEntity<ApiResult<ProductResponse>> updateProduct(
            @PathVariable Long productId,
            @RequestPart("product") ProductRequest request,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
            @RequestParam(required = false) List<Long> imageIdsToDelete,
            @RequestParam UUID adminId) throws IOException {

        log.info("✏️ [ADMIN] Updating clothing product #{}", productId);
        ProductResponse response = adminProductService.updateProduct(productId, request, newImages, imageIdsToDelete, adminId);
        return ResponseEntity.ok(ApiResult.success("Clothing product updated successfully", response));
    }

    @PutMapping(value = "/clothing/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Replace all images for a clothing product", description = "Deletes all existing images and uploads new ones")
    public ResponseEntity<ApiResult<ProductResponse>> replaceClothingImages(
            @PathVariable Long productId,
            @RequestPart("images") List<MultipartFile> images) throws IOException {

        log.info("🖼️ [ADMIN] Replacing all images for clothing product #{}", productId);
        ProductResponse response = adminProductService.replaceAllClothingImages(productId, images);
        return ResponseEntity.ok(ApiResult.success("Images replaced successfully", response));
    }

    @PatchMapping("/clothing/{productId}/stock")
    @Operation(summary = "Update stock for a clothing product")
    public ResponseEntity<ApiResult<ProductResponse>> updateStock(
            @PathVariable Long productId,
            @RequestParam int stock) {

        log.info("📦 [ADMIN] Updating stock for clothing product #{} → {}", productId, stock);
        ProductResponse response = adminProductService.updateStock(productId, stock);
        return ResponseEntity.ok(ApiResult.success("Stock updated to " + stock, response));
    }

    @PatchMapping("/clothing/{productId}/toggle-availability")
    @Operation(summary = "Toggle availability of a clothing product")
    public ResponseEntity<ApiResult<ProductResponse>> toggleAvailability(@PathVariable Long productId) {
        log.info("🔄 [ADMIN] Toggling availability for clothing product #{}", productId);
        ProductResponse response = adminProductService.toggleAvailability(productId);
        return ResponseEntity.ok(ApiResult.success("Availability toggled", response));
    }

    @DeleteMapping("/clothing/{productId}")
    @Operation(summary = "Delete a clothing product", description = "Removes product and deletes all images from Cloudinary")
    public ResponseEntity<ApiResult<String>> removeProduct(@PathVariable Long productId) throws IOException {
        log.info("🗑️ [ADMIN] Removing clothing product #{}", productId);
        String message = adminProductService.removeProduct(productId);
        return ResponseEntity.ok(ApiResult.success(message));
    }

    // ═══════════════════════════════════════════════════════════
    // CLOTHING PRODUCTS — READ
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/clothing")
    @Operation(summary = "Get all clothing products")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getAllProducts()));
    }

    @GetMapping("/clothing/mine")
    @Operation(summary = "Get clothing products added by a specific admin")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getMyProducts() {
        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();
        return ResponseEntity.ok(ApiResult.success(adminProductService.getMyProducts(adminId)));
    }

    @GetMapping("/clothing/{productId}")
    @Operation(summary = "Get a clothing product by ID")
    public ResponseEntity<ApiResult<ProductResponse>> getProductById(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getProductById(productId)));
    }

    @GetMapping("/clothing/search")
    @Operation(summary = "Search clothing products by name")
    public ResponseEntity<ApiResult<List<ProductResponse>>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.searchByName(name)));
    }

    @GetMapping("/clothing/search/keyword")
    @Operation(summary = "Search clothing products by keyword (name, description, brand, etc.)")
    public ResponseEntity<ApiResult<List<ProductResponse>>> searchByKeyword(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.searchByKeyword(keyword)));
    }

    @GetMapping("/clothing/category/{category}")
    @Operation(summary = "Get clothing products by category")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getByCategory(category)));
    }

    @GetMapping("/clothing/subcategory/{subCategory}")
    @Operation(summary = "Get clothing products by sub-category")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getBySubCategory(@PathVariable SubCategory subCategory) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getBySubCategory(subCategory)));
    }

    @GetMapping("/clothing/size/{size}")
    @Operation(summary = "Get clothing products available in a specific size")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getBySize(@PathVariable ClothingSize size) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getBySize(size)));
    }

    @GetMapping("/clothing/color/{color}")
    @Operation(summary = "Get clothing products available in a specific color")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getByColor(@PathVariable ClothingColor color) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getByColor(color)));
    }

    @GetMapping("/clothing/available")
    @Operation(summary = "Get all available clothing products (in stock)")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getAvailableProducts() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getAvailableProducts()));
    }

    @GetMapping("/clothing/discounted")
    @Operation(summary = "Get all discounted clothing products")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getDiscountedProducts() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getDiscountedProducts()));
    }

    @GetMapping("/clothing/meta/categories")
    @Operation(summary = "Get all distinct clothing categories")
    public ResponseEntity<ApiResult<List<String>>> getAllCategories() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getAllCategories()));
    }

    @GetMapping("/clothing/meta/brands")
    @Operation(summary = "Get all distinct clothing brands")
    public ResponseEntity<ApiResult<List<String>>> getAllBrands() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getAllBrands()));
    }

    @GetMapping("/clothing/meta/materials")
    @Operation(summary = "Get all distinct clothing materials")
    public ResponseEntity<ApiResult<List<String>>> getAllMaterials() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getAllMaterials()));
    }

    // ═══════════════════════════════════════════════════════════
    // GENERAL PRODUCTS — WRITE
    // ═══════════════════════════════════════════════════════════

    @PostMapping(value = "/general", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Add a new general product",
        description = "Creates a general product, uploads images to Cloudinary, emails all users, and posts to Telegram channel"
    )
    public ResponseEntity<ApiResult<GeneralProductResponse>> addGeneralProduct(
            @RequestPart("product") GeneralProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) throws IOException {

        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();

        log.info("➕ [ADMIN] Adding general product: {}", request.getName());
        GeneralProductResponse response = adminProductService.addGeneralProduct(request, images, adminId);
        return ResponseEntity.ok(ApiResult.success("General product added successfully", response));
    }

    @PutMapping(value = "/general/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update a general product")
    public ResponseEntity<ApiResult<GeneralProductResponse>> updateGeneralProduct(
            @PathVariable Long productId,
            @RequestPart("product") GeneralProductRequest request,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
            @RequestParam(required = false) List<Long> imageIdsToDelete) throws IOException {

        AdminPrincipal principal = adminPrincipal();
        UUID adminId = principal.getOwnerId();

        log.info("✏️ [ADMIN] Updating general product #{}", productId);
        GeneralProductResponse response = adminProductService.updateGeneralProduct(productId, request, newImages, imageIdsToDelete, adminId);
        return ResponseEntity.ok(ApiResult.success("General product updated successfully", response));
    }

    @PutMapping(value = "/general/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Replace all images for a general product")
    public ResponseEntity<ApiResult<GeneralProductResponse>> replaceGeneralImages(
            @PathVariable Long productId,
            @RequestPart("images") List<MultipartFile> images) throws IOException {

        log.info("🖼️ [ADMIN] Replacing all images for general product #{}", productId);
        GeneralProductResponse response = adminProductService.replaceAllGeneralImages(productId, images);
        return ResponseEntity.ok(ApiResult.success("Images replaced successfully", response));
    }

    @PatchMapping("/general/{productId}/stock")
    @Operation(summary = "Update stock for a general product")
    public ResponseEntity<ApiResult<GeneralProductResponse>> updateGeneralStock(
            @PathVariable Long productId,
            @RequestParam int stock) {

        log.info("📦 [ADMIN] Updating stock for general product #{} → {}", productId, stock);
        GeneralProductResponse response = adminProductService.updateGeneralStock(productId, stock);
        return ResponseEntity.ok(ApiResult.success("Stock updated to " + stock, response));
    }

    @PatchMapping("/general/{productId}/toggle-availability")
    @Operation(summary = "Toggle availability of a general product")
    public ResponseEntity<ApiResult<GeneralProductResponse>> toggleGeneralAvailability(@PathVariable Long productId) {
        log.info("🔄 [ADMIN] Toggling availability for general product #{}", productId);
        GeneralProductResponse response = adminProductService.toggleGeneralAvailability(productId);
        return ResponseEntity.ok(ApiResult.success("Availability toggled", response));
    }

    @DeleteMapping("/general/{productId}")
    @Operation(summary = "Delete a general product", description = "Removes product and deletes all images from Cloudinary")
    public ResponseEntity<ApiResult<String>> removeGeneralProduct(@PathVariable Long productId) throws IOException {
        log.info("🗑️ [ADMIN] Removing general product #{}", productId);
        String message = adminProductService.removeGeneralProduct(productId);
        return ResponseEntity.ok(ApiResult.success(message));
    }

    // ═══════════════════════════════════════════════════════════
    // GENERAL PRODUCTS — READ
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/general")
    @Operation(summary = "Get all general products")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getAllGeneralProducts() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getAllGeneralProducts()));
    }

    @GetMapping("/general/mine")
    @Operation(summary = "Get general products added by a specific admin")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getMyGeneralProducts(@RequestParam UUID adminId) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getMyGeneralProducts(adminId)));
    }

    @GetMapping("/general/{productId}")
    @Operation(summary = "Get a general product by ID")
    public ResponseEntity<ApiResult<GeneralProductResponse>> getGeneralProductById(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getGeneralProductById(productId)));
    }

    @GetMapping("/general/search")
    @Operation(summary = "Search general products by name")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> searchGeneralByName(@RequestParam String name) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.searchGeneralByName(name)));
    }

    @GetMapping("/general/search/keyword")
    @Operation(summary = "Search general products by keyword")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> searchGeneralByKeyword(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.searchGeneralByKeyword(keyword)));
    }

    @GetMapping("/general/category/{category}")
    @Operation(summary = "Get general products by category")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getGeneralByCategory(category)));
    }

    @GetMapping("/general/subcategory/{subCategory}")
    @Operation(summary = "Get general products by sub-category")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralBySubCategory(@PathVariable String subCategory) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getGeneralBySubCategory(subCategory)));
    }

    @GetMapping("/general/tag/{tag}")
    @Operation(summary = "Get general products by tag")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralByTag(@PathVariable String tag) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getGeneralByTag(tag)));
    }

    @GetMapping("/general/price-range")
    @Operation(summary = "Get general products within a price range")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getGeneralByPriceRange(min, max)));
    }

    @GetMapping("/general/available")
    @Operation(summary = "Get all available general products")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getAvailableGeneralProducts() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getAvailableGeneralProducts()));
    }

    @GetMapping("/general/discounted")
    @Operation(summary = "Get all discounted general products")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getDiscountedGeneralProducts() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getDiscountedGeneralProducts()));
    }

    @GetMapping("/general/meta/categories")
    @Operation(summary = "Get all distinct general product categories")
    public ResponseEntity<ApiResult<List<String>>> getAllGeneralCategories() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getAllGeneralCategories()));
    }

    @GetMapping("/general/meta/brands")
    @Operation(summary = "Get all distinct general product brands")
    public ResponseEntity<ApiResult<List<String>>> getAllGeneralBrands() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getAllGeneralBrands()));
    }

    @GetMapping("/general/meta/tags")
    @Operation(summary = "Get all distinct general product tags")
    public ResponseEntity<ApiResult<List<String>>> getAllGeneralTags() {
        return ResponseEntity.ok(ApiResult.success(adminProductService.getAllGeneralTags()));
    }
}