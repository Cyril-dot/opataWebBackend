package com.beautyShop.Opata.Website.service;

import com.beautyShop.Opata.Website.dto.GeneralProductRequest;
import com.beautyShop.Opata.Website.dto.GeneralProductResponse;
import com.beautyShop.Opata.Website.dto.ProductRequest;
import com.beautyShop.Opata.Website.dto.ProductResponse;
import com.beautyShop.Opata.Website.entity.*;
import com.beautyShop.Opata.Website.entity.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository             productRepository;
    private final ProductImageRepository        productImageRepository;
    private final GeneralProductRepository      generalProductRepository;
    private final GeneralProductImageRepository generalProductImageRepository;
    private final AdminRepo                     shopOwnerRepository;
    private final UserRepo                      userRepository;
    private final CloudinaryService             cloudinaryService;
    private final EmailService                  emailService;
    private final TelegramBotService            telegramBotService;

    // ═══════════════════════════════════════════════════════════
    //  CLOTHING PRODUCT METHODS
    // ═══════════════════════════════════════════════════════════

    // ── ADD CLOTHING PRODUCT ─────────────────────────────────
    public ProductResponse addProduct(ProductRequest request,
                                      List<MultipartFile> images,
                                      UUID adminId) throws IOException {

        ShopOwner admin = shopOwnerRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory().toLowerCase())
                .subCategory(request.getSubCategory())
                .brand(request.getBrand())
                .availableSizes(request.getAvailableSizes())
                .availableColors(request.getAvailableColors())
                .material(request.getMaterial())
                .style(request.getStyle())
                .stock(request.getStock())
                .discountPercentage(request.getDiscountPercentage())
                .isAvailable(true)
                .addedBy(admin)
                .build();

        Product saved = productRepository.save(product);

        if (images != null && !images.isEmpty()) {
            List<ProductImage> productImages = uploadClothingImages(images, saved);
            productImageRepository.saveAll(productImages);
            saved.setImages(productImages);
        }

        System.out.println("✅ Clothing product added: [" + saved.getName() + "] | SubCategory: "
                + saved.getSubCategory() + " | Images: " + saved.getImages().size());

        // ── Email all users ───────────────────────────────────
        List<User> allUsers = userRepository.findAll();
        if (!allUsers.isEmpty()) {
            emailService.announceNewProductToAllUsers(allUsers, saved, admin.getShopName());
        }

        // ── Announce on Telegram channel ──────────────────────
        String sizes = saved.getAvailableSizes() != null && !saved.getAvailableSizes().isEmpty()
                ? saved.getAvailableSizes().stream().map(Enum::name).collect(Collectors.joining(", "))
                : "N/A";
        String colors = saved.getAvailableColors() != null && !saved.getAvailableColors().isEmpty()
                ? saved.getAvailableColors().stream().map(Enum::name).collect(Collectors.joining(", "))
                : "N/A";

        telegramBotService.announceNewProduct(
                saved.getName(),
                saved.getDescription(),
                saved.getFinalPrice().toString(),
                saved.getCategory(),
                saved.getSubCategory() != null ? saved.getSubCategory().name() : "N/A",
                sizes,
                colors,
                saved.getMaterial(),
                saved.getStyle(),
                saved.getPrimaryImageUrl(),
                saved.getId()
        );

        return mapToProductResponse(saved);
    }

    // ── UPDATE CLOTHING PRODUCT ──────────────────────────────
    public ProductResponse updateProduct(Long productId,
                                         ProductRequest request,
                                         List<MultipartFile> newImages,
                                         List<Long> imageIdsToDelete,
                                         UUID adminId) throws IOException {

        Product product = findProductById(productId);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory().toLowerCase());
        product.setSubCategory(request.getSubCategory());
        product.setBrand(request.getBrand());
        product.setAvailableSizes(request.getAvailableSizes());
        product.setAvailableColors(request.getAvailableColors());
        product.setMaterial(request.getMaterial());
        product.setStyle(request.getStyle());
        product.setStock(request.getStock());
        product.setDiscountPercentage(request.getDiscountPercentage());
        product.setIsAvailable(request.getIsAvailable());

        if (imageIdsToDelete != null && !imageIdsToDelete.isEmpty()) {
            for (Long imageId : imageIdsToDelete) {
                productImageRepository.findById(imageId).ifPresent(img -> {
                    try {
                        cloudinaryService.deleteImage(img.getImagePublicId());
                        productImageRepository.delete(img);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete image: " + imageId, e);
                    }
                });
            }
            product.getImages().removeIf(img -> imageIdsToDelete.contains(img.getId()));
        }

        if (newImages != null && !newImages.isEmpty()) {
            int currentMaxOrder = product.getImages().stream()
                    .mapToInt(ProductImage::getDisplayOrder)
                    .max().orElse(-1);
            List<ProductImage> addedImages = uploadClothingImages(newImages, product, currentMaxOrder + 1);
            productImageRepository.saveAll(addedImages);
            product.getImages().addAll(addedImages);
        }

        Product updated = productRepository.save(product);
        System.out.println("✅ Clothing product updated: [" + updated.getName() + "] | "
                + updated.getImages().size() + " image(s)");
        return mapToProductResponse(updated);
    }

    // ── REPLACE ALL CLOTHING IMAGES ──────────────────────────
    public ProductResponse replaceAllClothingImages(Long productId,
                                                    List<MultipartFile> newImages) throws IOException {
        Product product = findProductById(productId);

        for (ProductImage img : product.getImages()) {
            cloudinaryService.deleteImage(img.getImagePublicId());
        }
        productImageRepository.deleteAll(product.getImages());
        product.getImages().clear();

        if (newImages != null && !newImages.isEmpty()) {
            List<ProductImage> uploaded = uploadClothingImages(newImages, product);
            productImageRepository.saveAll(uploaded);
            product.setImages(uploaded);
        }

        return mapToProductResponse(productRepository.save(product));
    }

    // ── UPDATE CLOTHING STOCK ────────────────────────────────
    public ProductResponse updateStock(Long productId, int newStock) {
        Product product = findProductById(productId);
        product.setStock(newStock);
        product.setIsAvailable(newStock > 0);
        productRepository.save(product);
        System.out.println("📦 Stock updated for [" + product.getName() + "]: " + newStock + " units");
        return mapToProductResponse(product);
    }

    // ── TOGGLE CLOTHING AVAILABILITY ─────────────────────────
    public ProductResponse toggleAvailability(Long productId) {
        Product product = findProductById(productId);
        product.setIsAvailable(!product.getIsAvailable());
        productRepository.save(product);
        System.out.println("🔄 Product [" + product.getName() + "] availability: " + product.getIsAvailable());
        return mapToProductResponse(product);
    }

    // ── REMOVE CLOTHING PRODUCT ──────────────────────────────
    public String removeProduct(Long productId) throws IOException {
        Product product = findProductById(productId);
        for (ProductImage img : product.getImages()) {
            cloudinaryService.deleteImage(img.getImagePublicId());
        }
        productRepository.delete(product);
        System.out.println("🗑️  Clothing product removed: [" + product.getName() + "]");
        return "Product \"" + product.getName() + "\" removed successfully.";
    }

    // ── CLOTHING READ OPERATIONS ─────────────────────────────
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToProductResponse).collect(Collectors.toList());
    }

    public List<ProductResponse> getMyProducts(UUID adminId) {
        return productRepository.findByAddedById(adminId).stream()
                .map(this::mapToProductResponse).collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long productId) {
        return mapToProductResponse(findProductById(productId));
    }

    public List<ProductResponse> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::mapToProductResponse).collect(Collectors.toList());
    }

    public List<ProductResponse> searchByKeyword(String keyword) {
        return productRepository.searchByKeyword(keyword).stream()
                .map(this::mapToProductResponse).collect(Collectors.toList());
    }

    public List<ProductResponse> getByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category).stream()
                .map(this::mapToProductResponse).collect(Collectors.toList());
    }

    public List<ProductResponse> getBySubCategory(SubCategory subCategory) {
        return productRepository.findBySubCategory(subCategory).stream()
                .map(this::mapToProductResponse).collect(Collectors.toList());
    }

    public List<ProductResponse> getBySize(ClothingSize size) {
        return productRepository.findByAvailableSizesContaining(size).stream()
                .map(this::mapToProductResponse).collect(Collectors.toList());
    }

    public List<ProductResponse> getByColor(ClothingColor color) {
        return productRepository.findByAvailableColorsContaining(color).stream()
                .map(this::mapToProductResponse).collect(Collectors.toList());
    }

    public List<ProductResponse> getAvailableProducts() {
        return productRepository.findByIsAvailableTrue().stream()
                .map(this::mapToProductResponse).collect(Collectors.toList());
    }

    public List<ProductResponse> getDiscountedProducts() {
        return productRepository.findByDiscountPercentageGreaterThan(BigDecimal.ZERO).stream()
                .map(this::mapToProductResponse).collect(Collectors.toList());
    }

    public List<String> getAllCategories() { return productRepository.findAllCategories(); }
    public List<String> getAllBrands()     { return productRepository.findAllBrands(); }
    public List<String> getAllMaterials()  { return productRepository.findAllMaterials(); }


    // ═══════════════════════════════════════════════════════════
    //  GENERAL PRODUCT METHODS
    // ═══════════════════════════════════════════════════════════

    // ── ADD GENERAL PRODUCT ──────────────────────────────────
    public GeneralProductResponse addGeneralProduct(GeneralProductRequest request,
                                                    List<MultipartFile> images,
                                                    UUID adminId) throws IOException {

        ShopOwner admin = shopOwnerRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        GeneralProduct product = GeneralProduct.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory().toLowerCase())
                .subCategory(request.getSubCategory())
                .brand(request.getBrand())
                .sku(request.getSku())
                .stock(request.getStock())
                .unit(request.getUnit())
                .weightKg(request.getWeightKg())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .discountPercentage(request.getDiscountPercentage())
                .tags(request.getTags())
                .attributes(request.getAttributes())
                .isAvailable(true)
                .addedBy(admin)
                .build();

        GeneralProduct saved = generalProductRepository.save(product);

        if (images != null && !images.isEmpty()) {
            List<GeneralProductImage> productImages = uploadGeneralImages(images, saved);
            generalProductImageRepository.saveAll(productImages);
            saved.setImages(productImages);
        }

        System.out.println("✅ General product added: [" + saved.getName() + "] | Category: "
                + saved.getCategory() + " | Images: " + saved.getImages().size());

        // ── Email all users ───────────────────────────────────
        List<User> allUsers = userRepository.findAll();
        if (!allUsers.isEmpty()) {
            emailService.announceNewGeneralProductToAllUsers(allUsers, saved, admin.getShopName());
        }

        // ── Announce on Telegram channel ──────────────────────
        String tags = saved.getTags() != null && !saved.getTags().isEmpty()
                ? String.join(", ", saved.getTags())
                : "N/A";

        telegramBotService.announceNewGeneralProduct(
                saved.getName(),
                saved.getDescription(),
                saved.getFinalPrice().toString(),
                saved.getCategory(),
                saved.getSubCategory(),
                tags,
                saved.getPrimaryImageUrl(),
                saved.getId()
        );

        return mapToGeneralProductResponse(saved);
    }

    // ── UPDATE GENERAL PRODUCT ───────────────────────────────
    public GeneralProductResponse updateGeneralProduct(Long productId,
                                                       GeneralProductRequest request,
                                                       List<MultipartFile> newImages,
                                                       List<Long> imageIdsToDelete,
                                                       UUID adminId) throws IOException {

        GeneralProduct product = findGeneralProductById(productId);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory().toLowerCase());
        product.setSubCategory(request.getSubCategory());
        product.setBrand(request.getBrand());
        product.setSku(request.getSku());
        product.setStock(request.getStock());
        product.setUnit(request.getUnit());
        product.setWeightKg(request.getWeightKg());
        product.setLengthCm(request.getLengthCm());
        product.setWidthCm(request.getWidthCm());
        product.setHeightCm(request.getHeightCm());
        product.setDiscountPercentage(request.getDiscountPercentage());
        product.setTags(request.getTags());
        product.setAttributes(request.getAttributes());
        product.setIsAvailable(request.getIsAvailable());

        if (imageIdsToDelete != null && !imageIdsToDelete.isEmpty()) {
            for (Long imageId : imageIdsToDelete) {
                generalProductImageRepository.findById(imageId).ifPresent(img -> {
                    try {
                        cloudinaryService.deleteImage(img.getImagePublicId());
                        generalProductImageRepository.delete(img);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete image: " + imageId, e);
                    }
                });
            }
            product.getImages().removeIf(img -> imageIdsToDelete.contains(img.getId()));
        }

        if (newImages != null && !newImages.isEmpty()) {
            int currentMaxOrder = product.getImages().stream()
                    .mapToInt(GeneralProductImage::getDisplayOrder)
                    .max().orElse(-1);
            List<GeneralProductImage> addedImages = uploadGeneralImages(newImages, product, currentMaxOrder + 1);
            generalProductImageRepository.saveAll(addedImages);
            product.getImages().addAll(addedImages);
        }

        GeneralProduct updated = generalProductRepository.save(product);
        System.out.println("✅ General product updated: [" + updated.getName() + "] | "
                + updated.getImages().size() + " image(s)");
        return mapToGeneralProductResponse(updated);
    }

    // ── REPLACE ALL GENERAL PRODUCT IMAGES ───────────────────
    public GeneralProductResponse replaceAllGeneralImages(Long productId,
                                                          List<MultipartFile> newImages) throws IOException {
        GeneralProduct product = findGeneralProductById(productId);

        for (GeneralProductImage img : product.getImages()) {
            cloudinaryService.deleteImage(img.getImagePublicId());
        }
        generalProductImageRepository.deleteAll(product.getImages());
        product.getImages().clear();

        if (newImages != null && !newImages.isEmpty()) {
            List<GeneralProductImage> uploaded = uploadGeneralImages(newImages, product);
            generalProductImageRepository.saveAll(uploaded);
            product.setImages(uploaded);
        }

        return mapToGeneralProductResponse(generalProductRepository.save(product));
    }

    // ── UPDATE GENERAL PRODUCT STOCK ─────────────────────────
    public GeneralProductResponse updateGeneralStock(Long productId, int newStock) {
        GeneralProduct product = findGeneralProductById(productId);
        product.setStock(newStock);
        product.setIsAvailable(newStock > 0);
        generalProductRepository.save(product);
        System.out.println("📦 General stock updated for [" + product.getName() + "]: " + newStock + " units");
        return mapToGeneralProductResponse(product);
    }

    // ── TOGGLE GENERAL PRODUCT AVAILABILITY ──────────────────
    public GeneralProductResponse toggleGeneralAvailability(Long productId) {
        GeneralProduct product = findGeneralProductById(productId);
        product.setIsAvailable(!product.getIsAvailable());
        generalProductRepository.save(product);
        System.out.println("🔄 General product [" + product.getName() + "] availability: " + product.getIsAvailable());
        return mapToGeneralProductResponse(product);
    }

    // ── REMOVE GENERAL PRODUCT ───────────────────────────────
    public String removeGeneralProduct(Long productId) throws IOException {
        GeneralProduct product = findGeneralProductById(productId);
        for (GeneralProductImage img : product.getImages()) {
            cloudinaryService.deleteImage(img.getImagePublicId());
        }
        generalProductRepository.delete(product);
        System.out.println("🗑️  General product removed: [" + product.getName() + "]");
        return "Product \"" + product.getName() + "\" removed successfully.";
    }

    // ── GENERAL PRODUCT READ OPERATIONS ──────────────────────
    public List<GeneralProductResponse> getAllGeneralProducts() {
        return generalProductRepository.findAll().stream()
                .map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    public List<GeneralProductResponse> getMyGeneralProducts(UUID adminId) {
        return generalProductRepository.findByAddedById(adminId).stream()
                .map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    public GeneralProductResponse getGeneralProductById(Long productId) {
        return mapToGeneralProductResponse(findGeneralProductById(productId));
    }

    public List<GeneralProductResponse> searchGeneralByName(String name) {
        return generalProductRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    public List<GeneralProductResponse> searchGeneralByKeyword(String keyword) {
        return generalProductRepository.searchByKeyword(keyword).stream()
                .map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    public List<GeneralProductResponse> getGeneralByCategory(String category) {
        return generalProductRepository.findByCategoryIgnoreCase(category).stream()
                .map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    public List<GeneralProductResponse> getGeneralBySubCategory(String subCategory) {
        return generalProductRepository.findBySubCategoryIgnoreCase(subCategory).stream()
                .map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    public List<GeneralProductResponse> getAvailableGeneralProducts() {
        return generalProductRepository.findByIsAvailableTrue().stream()
                .map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    public List<GeneralProductResponse> getDiscountedGeneralProducts() {
        return generalProductRepository.findByDiscountPercentageGreaterThan(BigDecimal.ZERO).stream()
                .map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    public List<GeneralProductResponse> getGeneralByTag(String tag) {
        return generalProductRepository.findByTagsContaining(tag).stream()
                .map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    public List<GeneralProductResponse> getGeneralByPriceRange(BigDecimal min, BigDecimal max) {
        return generalProductRepository.findByPriceBetween(min, max).stream()
                .map(this::mapToGeneralProductResponse).collect(Collectors.toList());
    }

    public List<String> getAllGeneralCategories() { return generalProductRepository.findAllCategories(); }
    public List<String> getAllGeneralBrands()     { return generalProductRepository.findAllBrands(); }
    public List<String> getAllGeneralTags()       { return generalProductRepository.findAllTags(); }


    // ═══════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    private List<ProductImage> uploadClothingImages(List<MultipartFile> files,
                                                    Product product) throws IOException {
        return uploadClothingImages(files, product, 0);
    }

    private List<ProductImage> uploadClothingImages(List<MultipartFile> files,
                                                    Product product,
                                                    int startOrder) throws IOException {
        List<ProductImage> result = new ArrayList<>();
        int order = startOrder;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            Map<String, Object> uploadResult = cloudinaryService.uploadImage(file, "beautyShop/clothing");
            result.add(ProductImage.builder()
                    .imageUrl((String) uploadResult.get("secure_url"))
                    .imagePublicId((String) uploadResult.get("public_id"))
                    .displayOrder(order++)
                    .product(product)
                    .build());
        }
        return result;
    }

    private List<GeneralProductImage> uploadGeneralImages(List<MultipartFile> files,
                                                          GeneralProduct product) throws IOException {
        return uploadGeneralImages(files, product, 0);
    }

    private List<GeneralProductImage> uploadGeneralImages(List<MultipartFile> files,
                                                          GeneralProduct product,
                                                          int startOrder) throws IOException {
        List<GeneralProductImage> result = new ArrayList<>();
        int order = startOrder;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            Map<String, Object> uploadResult = cloudinaryService.uploadImage(file, "beautyShop/general");
            result.add(GeneralProductImage.builder()
                    .imageUrl((String) uploadResult.get("secure_url"))
                    .imagePublicId((String) uploadResult.get("public_id"))
                    .displayOrder(order++)
                    .generalProduct(product)
                    .build());
        }
        return result;
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Clothing product not found with id: " + id));
    }

    private GeneralProduct findGeneralProductById(Long id) {
        return generalProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("General product not found with id: " + id));
    }

    ProductResponse mapToProductResponse(Product p) {
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

    GeneralProductResponse mapToGeneralProductResponse(GeneralProduct p) {
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