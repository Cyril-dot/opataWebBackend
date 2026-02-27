package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.Config.Security.UserPrincipal;
import com.beautyShop.Opata.Website.dto.GeneralProductResponse;
import com.beautyShop.Opata.Website.dto.ProductResponse;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.entity.ClothingColor;
import com.beautyShop.Opata.Website.entity.ClothingSize;
import com.beautyShop.Opata.Website.entity.SubCategory;
import com.beautyShop.Opata.Website.service.UserProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products (Public)", description = "Public product browsing and search — no authentication required")
public class UserProductController {

    private final UserProductService userProductService;

    // ═══════════════════════════════════════════════════════════
    // CLOTHING PRODUCTS — BROWSE
    // ═══════════════════════════════════════════════════════════


    private UserPrincipal userPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in SecurityContext");
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            log.error("Invalid principal type: {}", principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("Invalid authentication principal");
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;
        log.debug("Successfully retrieved UserPrincipal for user: {} (ID: {})",
                userPrincipal.getUsername(), userPrincipal.getUserId());

        return userPrincipal;
    }


    @GetMapping("/clothing")
    @Operation(summary = "Get all clothing products")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getAllProducts()));
    }

    @GetMapping("/clothing/available")
    @Operation(summary = "Get all available clothing products (in stock, not hidden)")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getAvailableProducts() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getAvailableProducts()));
    }

    @GetMapping("/clothing/in-stock")
    @Operation(summary = "Get clothing products with stock greater than 0")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getInStockProducts() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getInStockProducts()));
    }

    @GetMapping("/clothing/discounted")
    @Operation(summary = "Get discounted clothing products")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getDiscountedProducts() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getDiscountedProducts()));
    }

    @GetMapping("/clothing/{id}")
    @Operation(summary = "Get a single clothing product by ID")
    public ResponseEntity<ApiResult<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getProductById(id)));
    }

    // ═══════════════════════════════════════════════════════════
    // CLOTHING PRODUCTS — FILTER
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/clothing/category/{category}")
    @Operation(summary = "Filter clothing by category")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getByCategory(category)));
    }

    @GetMapping("/clothing/subcategory/{subCategory}")
    @Operation(summary = "Filter clothing by sub-category")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getBySubCategory(@PathVariable SubCategory subCategory) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getBySubCategory(subCategory)));
    }

    @GetMapping("/clothing/brand/{brand}")
    @Operation(summary = "Filter clothing by brand")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getByBrand(brand)));
    }

    @GetMapping("/clothing/size/{size}")
    @Operation(summary = "Filter clothing by size (e.g. S, M, L, XL)")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getBySize(@PathVariable ClothingSize size) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getBySize(size)));
    }

    @GetMapping("/clothing/color/{color}")
    @Operation(summary = "Filter clothing by color")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getByColor(@PathVariable ClothingColor color) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getByColor(color)));
    }

    @GetMapping("/clothing/material/{material}")
    @Operation(summary = "Filter clothing by material")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getByMaterial(@PathVariable String material) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getByMaterial(material)));
    }

    @GetMapping("/clothing/price-range")
    @Operation(summary = "Filter clothing by price range", description = "Returns clothing products priced between min and max")
    public ResponseEntity<ApiResult<List<ProductResponse>>> getByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getByPriceRange(min, max)));
    }

    @GetMapping("/clothing/filter")
    @Operation(
        summary = "Filter clothing by category + price range",
        description = "Combined filter: category and price range together"
    )
    public ResponseEntity<ApiResult<List<ProductResponse>>> getByCategoryAndPriceRange(
            @RequestParam String category,
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        return ResponseEntity.ok(ApiResult.success(
                userProductService.getByCategoryAndPriceRange(category, min, max)));
    }

    // ═══════════════════════════════════════════════════════════
    // CLOTHING PRODUCTS — SEARCH
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/clothing/search")
    @Operation(summary = "Search clothing by name")
    public ResponseEntity<ApiResult<List<ProductResponse>>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(ApiResult.success(userProductService.searchByName(name)));
    }

    @GetMapping("/clothing/search/keyword")
    @Operation(summary = "Search clothing by keyword (name, description, brand, category)")
    public ResponseEntity<ApiResult<List<ProductResponse>>> searchByKeyword(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResult.success(userProductService.searchByKeyword(keyword)));
    }

    @GetMapping("/clothing/search/name-category")
    @Operation(summary = "Search clothing by name and category together")
    public ResponseEntity<ApiResult<List<ProductResponse>>> searchByNameAndCategory(
            @RequestParam String name,
            @RequestParam String category) {
        return ResponseEntity.ok(ApiResult.success(
                userProductService.searchByNameAndCategory(name, category)));
    }

    @GetMapping("/clothing/search/name-brand")
    @Operation(summary = "Search clothing by name and brand together")
    public ResponseEntity<ApiResult<List<ProductResponse>>> searchByNameAndBrand(
            @RequestParam String name,
            @RequestParam String brand) {
        return ResponseEntity.ok(ApiResult.success(
                userProductService.searchByNameAndBrand(name, brand)));
    }

    // ═══════════════════════════════════════════════════════════
    // CLOTHING PRODUCTS — META / DROPDOWN DATA
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/clothing/meta/categories")
    @Operation(summary = "Get all distinct clothing categories for dropdown")
    public ResponseEntity<ApiResult<List<String>>> getAllCategories() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getAllCategories()));
    }

    @GetMapping("/clothing/meta/brands")
    @Operation(summary = "Get all distinct clothing brands for dropdown")
    public ResponseEntity<ApiResult<List<String>>> getAllBrands() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getAllBrands()));
    }

    @GetMapping("/clothing/meta/materials")
    @Operation(summary = "Get all distinct clothing materials for dropdown")
    public ResponseEntity<ApiResult<List<String>>> getAllMaterials() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getAllMaterials()));
    }

    // ═══════════════════════════════════════════════════════════
    // GENERAL PRODUCTS — BROWSE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/general")
    @Operation(summary = "Get all general products")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getAllGeneralProducts() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getAllGeneralProducts()));
    }

    @GetMapping("/general/available")
    @Operation(summary = "Get available general products")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getAvailableGeneralProducts() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getAvailableGeneralProducts()));
    }

    @GetMapping("/general/in-stock")
    @Operation(summary = "Get general products with stock greater than 0")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralInStockProducts() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getGeneralInStockProducts()));
    }

    @GetMapping("/general/discounted")
    @Operation(summary = "Get discounted general products")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getDiscountedGeneralProducts() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getDiscountedGeneralProducts()));
    }

    @GetMapping("/general/{id}")
    @Operation(summary = "Get a single general product by ID")
    public ResponseEntity<ApiResult<GeneralProductResponse>> getGeneralProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getGeneralProductById(id)));
    }

    // ═══════════════════════════════════════════════════════════
    // GENERAL PRODUCTS — FILTER
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/general/category/{category}")
    @Operation(summary = "Filter general products by category")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getGeneralByCategory(category)));
    }

    @GetMapping("/general/subcategory/{subCategory}")
    @Operation(summary = "Filter general products by sub-category")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralBySubCategory(
            @PathVariable String subCategory) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getGeneralBySubCategory(subCategory)));
    }

    @GetMapping("/general/brand/{brand}")
    @Operation(summary = "Filter general products by brand")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getGeneralByBrand(brand)));
    }

    @GetMapping("/general/tag/{tag}")
    @Operation(summary = "Filter general products by tag")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralByTag(@PathVariable String tag) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getGeneralByTag(tag)));
    }

    @GetMapping("/general/price-range")
    @Operation(summary = "Filter general products by price range")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getGeneralByPriceRange(min, max)));
    }

    @GetMapping("/general/filter")
    @Operation(summary = "Filter general products by category + price range")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralByCategoryAndPriceRange(
            @RequestParam String category,
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        return ResponseEntity.ok(ApiResult.success(
                userProductService.getGeneralByCategoryAndPriceRange(category, min, max)));
    }

    @GetMapping("/general/attribute")
    @Operation(
        summary = "Filter general products by custom attribute",
        description = "e.g. key=SkinType&value=Oily or key=Warranty&value=2 years"
    )
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> getGeneralByAttribute(
            @RequestParam String key,
            @RequestParam String value) {
        return ResponseEntity.ok(ApiResult.success(userProductService.getGeneralByAttribute(key, value)));
    }

    // ═══════════════════════════════════════════════════════════
    // GENERAL PRODUCTS — SEARCH
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/general/search")
    @Operation(summary = "Search general products by name")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> searchGeneralByName(
            @RequestParam String name) {
        return ResponseEntity.ok(ApiResult.success(userProductService.searchGeneralByName(name)));
    }

    @GetMapping("/general/search/keyword")
    @Operation(summary = "Search general products by keyword")
    public ResponseEntity<ApiResult<List<GeneralProductResponse>>> searchGeneralByKeyword(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResult.success(userProductService.searchGeneralByKeyword(keyword)));
    }

    // ═══════════════════════════════════════════════════════════
    // GENERAL PRODUCTS — META / DROPDOWN DATA
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/general/meta/categories")
    @Operation(summary = "Get all distinct general product categories")
    public ResponseEntity<ApiResult<List<String>>> getAllGeneralCategories() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getAllGeneralCategories()));
    }

    @GetMapping("/general/meta/subcategories")
    @Operation(summary = "Get all distinct general product sub-categories")
    public ResponseEntity<ApiResult<List<String>>> getAllGeneralSubCategories() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getAllGeneralSubCategories()));
    }

    @GetMapping("/general/meta/brands")
    @Operation(summary = "Get all distinct general product brands")
    public ResponseEntity<ApiResult<List<String>>> getAllGeneralBrands() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getAllGeneralBrands()));
    }

    @GetMapping("/general/meta/tags")
    @Operation(summary = "Get all distinct general product tags")
    public ResponseEntity<ApiResult<List<String>>> getAllGeneralTags() {
        return ResponseEntity.ok(ApiResult.success(userProductService.getAllGeneralTags()));
    }
}