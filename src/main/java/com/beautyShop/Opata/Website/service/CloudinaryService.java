package com.beautyShop.Opata.Website.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    // ─── Max file size before compression kicks in (500KB) ───
    private static final long MAX_SIZE_BYTES = 500 * 1024;

    // ─────────────────────────────────────────────────────────
    // UPLOAD IMAGE
    // Compresses if over 500KB, checks for duplicates first
    // ─────────────────────────────────────────────────────────
    public Map uploadImage(MultipartFile file, String folder) throws IOException {

        // 1. Check for duplicate using original filename
        String originalFilename = file.getOriginalFilename();
        String publicIdToCheck = folder + "/" + getFilenameWithoutExtension(originalFilename);

        if (imageExistsOnCloudinary(publicIdToCheck)) {
            System.out.println("⚠️  Duplicate detected: " + publicIdToCheck + " already exists on Cloudinary. Skipping upload.");
            // Return existing image details instead of re-uploading
            try {
                return cloudinary.api().resource(publicIdToCheck, ObjectUtils.emptyMap());
            } catch (Exception e) {
                throw new IOException("Failed to fetch existing image details", e);
            }
        }

        // 2. Compress image if it exceeds 500KB
        byte[] imageBytes = file.getBytes();
        if (imageBytes.length > MAX_SIZE_BYTES) {
            System.out.println("📦 Image is " + (imageBytes.length / 1024) + "KB — compressing to under 500KB...");
            imageBytes = compressImage(imageBytes);
            System.out.println("✅ Compressed to " + (imageBytes.length / 1024) + "KB");
        } else {
            System.out.println("✅ Image is " + (imageBytes.length / 1024) + "KB — no compression needed.");
        }

        // 3. Upload to Cloudinary with built-in optimization
        Map uploadResult = cloudinary.uploader().upload(
                imageBytes,
                ObjectUtils.asMap(
                        "folder",         folder,
                        "resource_type",  "image",
                        "overwrite",      false,          // prevent overwriting existing
                        "quality",        "auto",         // q_auto — Cloudinary picks best quality
                        "fetch_format",   "auto",         // f_auto — serves WebP/AVIF where supported
                        "flags",          "progressive"   // progressive JPEG for faster perceived load
                )
        );

        System.out.println("🚀 Uploaded successfully!");
        System.out.println("   URL        : " + uploadResult.get("secure_url"));
        System.out.println("   Public ID  : " + uploadResult.get("public_id"));
        System.out.println("   Format     : " + uploadResult.get("format"));
        System.out.println("   Size       : " + uploadResult.get("bytes") + " bytes");
        System.out.println("   Dimensions : " + uploadResult.get("width") + "x" + uploadResult.get("height"));

        return uploadResult;
    }

    // ─────────────────────────────────────────────────────────
    // DELETE IMAGE
    // ─────────────────────────────────────────────────────────
    public Map deleteImage(String publicId) throws IOException {
        System.out.println("🗑️  Deleting image from Cloudinary: " + publicId);
        Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        System.out.println("   Result: " + result.get("result"));
        return result;
    }

    // ─────────────────────────────────────────────────────────
    // GET SINGLE IMAGE URL (with q_auto + f_auto optimization)
    // Returns the optimized URL for a given public ID
    // ─────────────────────────────────────────────────────────
    public String getOptimizedImageUrl(String publicId) {
        String url = cloudinary.url()
                .transformation(new com.cloudinary.Transformation()
                        .quality("auto")      // q_auto
                        .fetchFormat("auto")  // f_auto
                )
                .generate(publicId);

        System.out.println("🖼️  Optimized image URL for [" + publicId + "]:");
        System.out.println("   " + url);
        return url;
    }

    // ─────────────────────────────────────────────────────────
    // LIST ALL IMAGES IN A FOLDER
    // Returns all images stored under a given Cloudinary folder
    // ─────────────────────────────────────────────────────────
    public List<Map> listAllImagesInFolder(String folder) throws Exception {
        Map result = cloudinary.api().resources(
                ObjectUtils.asMap(
                        "type",        "upload",
                        "prefix",      folder,
                        "max_results", 100
                )
        );

        List<Map> resources = (List<Map>) result.get("resources");

        System.out.println("📂 Images in folder [" + folder + "]:");
        System.out.println("   Total found: " + resources.size());
        System.out.println("   ─────────────────────────────────────────");
        for (Map image : resources) {
            System.out.println("   Public ID  : " + image.get("public_id"));
            System.out.println("   URL        : " + image.get("secure_url"));
            System.out.println("   Format     : " + image.get("format"));
            System.out.println("   Size       : " + image.get("bytes") + " bytes");
            System.out.println("   Dimensions : " + image.get("width") + "x" + image.get("height"));
            System.out.println("   Created    : " + image.get("created_at"));
            System.out.println("   ─────────────────────────────────────────");
        }

        return resources;
    }

    // ─────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────

    /**
     * Compress image bytes to stay under 500KB.
     * Progressively reduces JPEG quality until small enough.
     */
    private byte[] compressImage(byte[] originalBytes) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(originalBytes));

        // Convert transparent images (PNG) to RGB for JPEG compression
        if (bufferedImage.getType() == BufferedImage.TYPE_4BYTE_ABGR
                || bufferedImage.getType() == BufferedImage.TYPE_INT_ARGB) {
            BufferedImage rgbImage = new BufferedImage(
                    bufferedImage.getWidth(),
                    bufferedImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g = rgbImage.createGraphics();
            g.drawImage(bufferedImage, 0, 0, Color.WHITE, null);
            g.dispose();
            bufferedImage = rgbImage;
        }

        // Reduce quality step by step until under 500KB
        float quality = 0.85f;
        byte[] compressed = originalBytes;

        while (compressed.length > MAX_SIZE_BYTES && quality > 0.1f) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            var jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
            var jpegParams = jpegWriter.getDefaultWriteParam();
            jpegParams.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            jpegParams.setCompressionQuality(quality);

            try (var ios = ImageIO.createImageOutputStream(baos)) {
                jpegWriter.setOutput(ios);
                jpegWriter.write(null, new javax.imageio.IIOImage(bufferedImage, null, null), jpegParams);
            }
            jpegWriter.dispose();
            compressed = baos.toByteArray();
            quality -= 0.1f;
        }

        return compressed;
    }

    /**
     * Check if an image already exists on Cloudinary to avoid duplicates.
     */
    private boolean imageExistsOnCloudinary(String publicId) {
        try {
            cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Strip file extension. e.g. "dell-xps.jpg" → "dell-xps"
     */
    private String getFilenameWithoutExtension(String filename) {
        if (filename == null) return "unknown";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }
}