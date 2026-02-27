package com.beautyShop.Opata.Website.service;

import com.beautyShop.Opata.Website.dto.GeneralProductResponse;
import com.beautyShop.Opata.Website.dto.ProductResponse;
import com.beautyShop.Opata.Website.entity.GeneralProduct;
import com.beautyShop.Opata.Website.entity.Product;
import com.beautyShop.Opata.Website.entity.*;
import com.beautyShop.Opata.Website.entity.repo.GeneralProductRepository;
import com.beautyShop.Opata.Website.entity.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserProductService — CUSTOMER FACING
 * Handles: browse, search, filter — READ ONLY
 * Covers both Clothing Products and General Products
 */
@Service
@RequiredArgsConstructor
public class UserProductService {

    private final ProductRepository       productRepository;
    private final GeneralProductRepository generalProductRepository;
    private final CloudinaryService       cloudinaryService;


    // ═══════════════════════════════════════════════════════════
    //  CLOTHING PRODUCTS — READ OPERATIONS
    // ═══════════════════════════════════════════════════════════

    // ── GET ALL ──────────────────────────────────────────────
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── GET AVAILABLE ONLY ───────────────────────────────────
    public List<ProductResponse> getAvailableProducts() {
        return productRepository.findByIsAvailableTrue()
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── GET SINGLE ───────────────────────────────────────────
    public ProductResponse getProductById(Long id) {
        return mapToProductResponse(findProductById(id));
    }

    // ── IN STOCK ONLY ────────────────────────────────────────
    public List<ProductResponse> getInStockProducts() {
        return productRepository.findByStockGreaterThan(0)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── DISCOUNTED PRODUCTS ──────────────────────────────────
    public List<ProductResponse> getDiscountedProducts() {
        return productRepository.findByDiscountPercentageGreaterThan(BigDecimal.ZERO)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY CATEGORY ───────────────────────────────────
    public List<ProductResponse> getByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY SUB-CATEGORY ───────────────────────────────
    public List<ProductResponse> getBySubCategory(SubCategory subCategory) {
        return productRepository.findBySubCategory(subCategory)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY BRAND ──────────────────────────────────────
    public List<ProductResponse> getByBrand(String brand) {
        return productRepository.findByBrandIgnoreCase(brand)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY SIZE ───────────────────────────────────────
    public List<ProductResponse> getBySize(ClothingSize size) {
        return productRepository.findByAvailableSizesContaining(size)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY COLOR ──────────────────────────────────────
    public List<ProductResponse> getByColor(ClothingColor color) {
        return productRepository.findByAvailableColorsContaining(color)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY MATERIAL ───────────────────────────────────
    public List<ProductResponse> getByMaterial(String material) {
        return productRepository.findByNameContainingIgnoreCase(material)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── SEARCH BY NAME ───────────────────────────────────────
    public List<ProductResponse> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── FULL KEYWORD SEARCH ──────────────────────────────────
    public List<ProductResponse> searchByKeyword(String keyword) {
        return productRepository.searchByKeyword(keyword)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── SEARCH BY NAME + CATEGORY ────────────────────────────
    public List<ProductResponse> searchByNameAndCategory(String name, String category) {
        return productRepository.findByNameContainingIgnoreCaseAndCategoryIgnoreCase(name, category)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── SEARCH BY NAME + BRAND ───────────────────────────────
    public List<ProductResponse> searchByNameAndBrand(String name, String brand) {
        return productRepository.findByNameContainingIgnoreCaseAndBrandIgnoreCase(name, brand)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── FILTER BY PRICE RANGE ────────────────────────────────
    public List<ProductResponse> getByPriceRange(BigDecimal min, BigDecimal max) {
        return productRepository.findByPriceBetween(min, max)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── FILTER BY CATEGORY + PRICE RANGE ────────────────────
    public List<ProductResponse> getByCategoryAndPriceRange(String category, BigDecimal min, BigDecimal max) {
        return productRepository.findByCategoryIgnoreCaseAndPriceBetween(category, min, max)
                .stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }

    // ── DROPDOWN HELPERS ─────────────────────────────────────
    public List<String> getAllCategories()  { return productRepository.findAllCategories(); }
    public List<String> getAllBrands()      { return productRepository.findAllBrands(); }
    public List<String> getAllMaterials()   { return productRepository.findAllMaterials(); }


    // ═══════════════════════════════════════════════════════════
    //  GENERAL PRODUCTS — READ OPERATIONS
    // ═══════════════════════════════════════════════════════════

    // ── GET ALL ──────────────────────────────────────────────
    public List<GeneralProductResponse> getAllGeneralProducts() {
        return generalProductRepository.findAll()
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── GET AVAILABLE ONLY ───────────────────────────────────
    public List<GeneralProductResponse> getAvailableGeneralProducts() {
        return generalProductRepository.findByIsAvailableTrue()
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── GET SINGLE ───────────────────────────────────────────
    public GeneralProductResponse getGeneralProductById(Long id) {
        return mapToGeneralProductResponse(findGeneralProductById(id));
    }

    // ── IN STOCK ONLY ────────────────────────────────────────
    public List<GeneralProductResponse> getGeneralInStockProducts() {
        return generalProductRepository.findByStockGreaterThan(0)
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── DISCOUNTED PRODUCTS ──────────────────────────────────
    public List<GeneralProductResponse> getDiscountedGeneralProducts() {
        return generalProductRepository.findByDiscountPercentageGreaterThan(BigDecimal.ZERO)
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY CATEGORY ───────────────────────────────────
    public List<GeneralProductResponse> getGeneralByCategory(String category) {
        return generalProductRepository.findByCategoryIgnoreCase(category)
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY SUB-CATEGORY ───────────────────────────────
    public List<GeneralProductResponse> getGeneralBySubCategory(String subCategory) {
        return generalProductRepository.findBySubCategoryIgnoreCase(subCategory)
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY BRAND ──────────────────────────────────────
    public List<GeneralProductResponse> getGeneralByBrand(String brand) {
        return generalProductRepository.findByBrandIgnoreCase(brand)
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── BROWSE BY TAG ────────────────────────────────────────
    public List<GeneralProductResponse> getGeneralByTag(String tag) {
        return generalProductRepository.findByTagsContaining(tag)
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── SEARCH BY NAME ───────────────────────────────────────
    public List<GeneralProductResponse> searchGeneralByName(String name) {
        return generalProductRepository.findByNameContainingIgnoreCase(name)
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── FULL KEYWORD SEARCH ──────────────────────────────────
    public List<GeneralProductResponse> searchGeneralByKeyword(String keyword) {
        return generalProductRepository.searchByKeyword(keyword)
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── FILTER BY PRICE RANGE ────────────────────────────────
    public List<GeneralProductResponse> getGeneralByPriceRange(BigDecimal min, BigDecimal max) {
        return generalProductRepository.findByPriceBetween(min, max)
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── FILTER BY CATEGORY + PRICE RANGE ────────────────────
    public List<GeneralProductResponse> getGeneralByCategoryAndPriceRange(String category,
                                                                          BigDecimal min,
                                                                          BigDecimal max) {
        return generalProductRepository.findByCategoryIgnoreCaseAndPriceBetween(category, min, max)
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── SEARCH BY CUSTOM ATTRIBUTE ───────────────────────────
    // e.g. key="Skin Type", value="Oily" or key="Warranty", value="2 years"
    public List<GeneralProductResponse> getGeneralByAttribute(String key, String value) {
        return generalProductRepository.findByAttribute(key, value)
                .stream().map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    // ── DROPDOWN HELPERS ─────────────────────────────────────
    public List<String> getAllGeneralCategories()    { return generalProductRepository.findAllCategories(); }
    public List<String> getAllGeneralSubCategories() { return generalProductRepository.findAllSubCategories(); }
    public List<String> getAllGeneralBrands()        { return generalProductRepository.findAllBrands(); }
    public List<String> getAllGeneralTags()          { return generalProductRepository.findAllTags(); }


    // ═══════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    private GeneralProduct findGeneralProductById(Long id) {
        return generalProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("General product not found with id: " + id));
    }

    private ProductResponse mapToProductResponse(Product p) {
        List<String> imageUrls = p.getImages().stream()
                .map(img -> cloudinaryService.getOptimizedImageUrl(img.getImagePublicId()))
                .collect(Collectors.toList());

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .finalPrice(p.getFinalPrice())
                .discountPercentage(p.getDiscountPercentage())
                .category(p.getCategory())
                .subCategory(p.getSubCategory())
                .brand(p.getBrand())
                .availableSizes(p.getAvailableSizes())
                .availableColors(p.getAvailableColors())
                .material(p.getMaterial())
                .style(p.getStyle())
                .stock(p.getStock())
                .isAvailable(p.getIsAvailable())
                .imageUrl(imageUrls.isEmpty() ? null : imageUrls.getFirst())
                .imageUrls(imageUrls)
                .addedByAdmin(p.getAddedBy() != null ? p.getAddedBy().getName() : "N/A")
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private GeneralProductResponse mapToGeneralProductResponse(GeneralProduct p) {
        List<String> imageUrls = p.getImages().stream()
                .map(img -> cloudinaryService.getOptimizedImageUrl(img.getImagePublicId()))
                .collect(Collectors.toList());

        return GeneralProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .finalPrice(p.getFinalPrice())
                .discountPercentage(p.getDiscountPercentage())
                .category(p.getCategory())
                .subCategory(p.getSubCategory())
                .brand(p.getBrand())
                .sku(p.getSku())
                .stock(p.getStock())
                .unit(p.getUnit())
                .weightKg(p.getWeightKg())
                .lengthCm(p.getLengthCm())
                .widthCm(p.getWidthCm())
                .heightCm(p.getHeightCm())
                .isAvailable(p.getIsAvailable())
                .tags(p.getTags())
                .attributes(p.getAttributes())
                .imageUrl(imageUrls.isEmpty() ? null : imageUrls.getFirst())
                .imageUrls(imageUrls)
                .addedByAdmin(p.getAddedBy() != null ? p.getAddedBy().getName() : "N/A")
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}