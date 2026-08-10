package com.example.secdsp.infrastructure.cloudinary;

import com.example.secdsp.config.CloudinaryConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStartupValidator implements ApplicationRunner {

    private final CloudinaryConfig cloudinaryConfig;
    private final CloudinaryStorageGuard storageGuard;

    @Override
    public void run(ApplicationArguments args) {
        if (!storageGuard.isProductionProfile()) {
            if (cloudinaryConfig.isConfigured()) {
                log.info("Cloudinary configured — uploads persist to cloud (dev/local).");
            } else {
                log.warn(
                    "Cloudinary chưa cấu hình — dev dùng lưu local {} (chỉ máy local, không dùng cho prod).",
                    "uploads/"
                );
            }
            return;
        }

        if (cloudinaryConfig.isConfigured()) {
            log.info(
                "Cloudinary OK (cloud_name={}) — ảnh sản phẩm lưu bền vững, không phụ thuộc disk Railway.",
                cloudinaryConfig.getCloudName()
            );
            return;
        }

        log.error(
            "PRODUCTION: Cloudinary CHƯA cấu hình (cloud_name={}). "
                + "Upload ảnh sẽ FAIL. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET trên Railway.",
            cloudinaryConfig.getCloudName()
        );
    }
}
