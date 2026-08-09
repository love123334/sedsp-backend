package com.example.secdsp.infrastructure.storage;

import com.example.secdsp.common.exception.CloudinaryException;
import com.example.secdsp.config.UploadProperties;
import com.example.secdsp.modules.product.dto.response.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Filesystem fallback when Cloudinary is missing or misconfigured
 * (e.g. CLOUDINARY_CLOUD_NAME=SEDSP on Railway).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalImageStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final UploadProperties uploadProperties;

    public CloudinaryUploadResult store(MultipartFile file) {
        try {
            Path root = Paths.get(uploadProperties.getDir(), "products").toAbsolutePath().normalize();
            Files.createDirectories(root);

            String ext = extensionOf(file);
            String publicId = "local/products/" + UUID.randomUUID();
            String filename = publicId.substring("local/products/".length()) + "." + ext;
            Path target = root.resolve(filename).normalize();
            if (!target.startsWith(root)) {
                throw new CloudinaryException("Đường dẫn upload không hợp lệ");
            }

            Files.write(target, file.getBytes());

            String url = uploadProperties.resolvePublicBaseUrl()
                + "/uploads/products/"
                + filename;

            log.info("Stored product image locally: {}", url);
            return new CloudinaryUploadResult(url, publicId);
        } catch (IOException e) {
            throw new CloudinaryException("Không lưu được ảnh local: " + e.getMessage(), e);
        }
    }

    public void deleteIfLocal(String publicId) {
        if (publicId == null || !publicId.startsWith("local/products/")) {
            return;
        }
        try {
            Path root = Paths.get(uploadProperties.getDir(), "products").toAbsolutePath().normalize();
            String stem = publicId.substring("local/products/".length());
            try (var stream = Files.list(root)) {
                stream.filter(p -> p.getFileName().toString().startsWith(stem + "."))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                            // best-effort
                        }
                    });
            }
        } catch (IOException e) {
            log.warn("Failed to delete local image {}", publicId, e);
        }
    }

    private static String extensionOf(MultipartFile file) {
        String original = file.getOriginalFilename();
        String fromName = StringUtils.getFilenameExtension(original);
        if (fromName != null && ALLOWED_EXT.contains(fromName.toLowerCase(Locale.ROOT))) {
            return fromName.toLowerCase(Locale.ROOT);
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            return switch (contentType.toLowerCase(Locale.ROOT)) {
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                case "image/gif" -> "gif";
                default -> "jpg";
            };
        }
        return "jpg";
    }
}
