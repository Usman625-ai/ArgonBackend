package com.ecommerce.multivendor.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.multivendor.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder}")
    private String folder;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    /**
     * Upload a single image file to Cloudinary with compression.
     * Returns {url, publicId}
     */
    public Map<String, String> uploadImage(MultipartFile file, String subfolder) {
        validateImageFile(file);

        try {
            byte[] compressedBytes = compressImage(file);
            String publicId = folder + "/" + subfolder + "/" + UUID.randomUUID();

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(compressedBytes,
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "overwrite", true,
                            "resource_type", "image",
                            "quality", "auto:good",
                            "fetch_format", "auto"
                    )
            );

            String imageUrl = (String) result.get("secure_url");
            String resultPublicId = (String) result.get("public_id");

            log.info("Image uploaded successfully: {}", imageUrl);
            return Map.of("url", imageUrl, "publicId", resultPublicId);

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage());
            throw new BadRequestException("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Upload multiple images.
     */
    public List<Map<String, String>> uploadImages(List<MultipartFile> files, String subfolder) {
        List<Map<String, String>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadImage(file, subfolder));
        }
        return results;
    }

    /**
     * Delete image from Cloudinary by publicId.
     */
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary [{}]: {}", publicId, e.getMessage());
        }
    }

    /**
     * Delete multiple images.
     */
    public void deleteImages(List<String> publicIds) {
        publicIds.forEach(this::deleteImage);
    }

    /**
     * Extracts the Cloudinary public_id from a secure_url, e.g.
     * https://res.cloudinary.com/<cloud>/image/upload/v169.../folder/sub/abc123.jpg
     * -> folder/sub/abc123
     *
     * Returns null for anything that isn't a Cloudinary upload URL (external
     * seed/demo image URLs, blank strings, etc.) — callers can pass the result
     * straight to deleteImage(), which already no-ops safely on null.
     */
    public String extractPublicId(String url) {
        if (url == null || url.isBlank()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile(".*/upload/(?:v\\d+/)?(.+)\\.[a-zA-Z0-9]+(?:\\?.*)?$")
                .matcher(url);
        return m.matches() ? m.group(1) : null;
    }

    /**
     * Convenience wrapper for entities that only store the image URL (no
     * separate publicId column) — e.g. Category.imageUrl, User.profileImage,
     * User.shopLogo/shopBanner. Safe to call with any URL, including
     * non-Cloudinary ones (silently does nothing) or null/blank (no-ops).
     */
    public void deleteImageByUrl(String url) {
        String publicId = extractPublicId(url);
        if (publicId != null) deleteImage(publicId);
    }

    /**
     * Upload from a URL (e.g. external product image).
     */
    public Map<String, String> uploadFromUrl(String imageUrl, String subfolder) {
        try {
            String publicId = folder + "/" + subfolder + "/" + UUID.randomUUID();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(imageUrl,
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "image"
                    )
            );
            return Map.of(
                    "url", (String) result.get("secure_url"),
                    "publicId", (String) result.get("public_id")
            );
        } catch (IOException e) {
            throw new BadRequestException("Failed to upload image from URL: " + e.getMessage());
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Image size exceeds 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("Invalid image type. Allowed: JPEG, PNG, WebP, GIF");
        }
    }

    /**
     * Compress image using Thumbnailator before upload.
     * Resizes to max 1200px width while maintaining aspect ratio.
     */
    private byte[] compressImage(MultipartFile file) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(file.getInputStream())
                .size(1200, 1200)
                .keepAspectRatio(true)
                .outputQuality(0.85)
                .toOutputStream(outputStream);
        return outputStream.toByteArray();
    }
}
