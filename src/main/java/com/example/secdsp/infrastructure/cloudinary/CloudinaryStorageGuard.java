package com.example.secdsp.infrastructure.cloudinary;

import com.example.secdsp.common.exception.CloudinaryException;
import com.example.secdsp.config.CloudinaryConfig;
import com.example.secdsp.modules.product.dto.response.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class CloudinaryStorageGuard {

    private final CloudinaryConfig cloudinaryConfig;
    private final Environment environment;

    public boolean isProductionProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    public boolean allowLocalImageFallback() {
        return !isProductionProfile();
    }

    public void requireCloudinaryForUpload() {
        if (cloudinaryConfig.isConfigured()) {
            return;
        }
        if (isProductionProfile()) {
            throw new CloudinaryException(
                "Cloudinary chưa cấu hình trên Railway. "
                    + "Set CLOUDINARY_CLOUD_NAME (lowercase), CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET. "
                    + "Không lưu ảnh local trên prod — disk Railway bị xóa mỗi lần deploy."
            );
        }
    }

    public CloudinaryUploadResult fallbackToLocalOrThrow(
        String reason,
        java.util.function.Supplier<CloudinaryUploadResult> localStore
    ) {
        if (allowLocalImageFallback()) {
            return localStore.get();
        }
        throw new CloudinaryException(
            "Upload Cloudinary thất bại trên prod: " + reason
                + ". Kiểm tra CLOUDINARY_* trên Railway (không dùng SEDSP/your_cloud_name)."
        );
    }
}
