package com.example.secdsp.infrastructure.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.secdsp.common.exception.CloudinaryException;
import com.example.secdsp.config.CloudinaryConfig;
import com.example.secdsp.infrastructure.storage.LocalImageStorageService;
import com.example.secdsp.modules.product.dto.response.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final CloudinaryConfig cloudinaryConfig;
    private final LocalImageStorageService localImageStorageService;
    private final CloudinaryStorageGuard storageGuard;

    @Override
    public CloudinaryUploadResult uploadImage(MultipartFile file) {
        storageGuard.requireCloudinaryForUpload();

        if (!cloudinaryConfig.isConfigured()) {
            log.warn(
                "Cloudinary chưa cấu hình (cloud_name={}). Dùng lưu ảnh local (dev only).",
                cloudinaryConfig.getCloudName()
            );
            return localImageStorageService.store(file);
        }

        try {
            @SuppressWarnings("rawtypes")
            Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder", "secdsp/products",
                    "resource_type", "image",
                    "overwrite", false,
                    "unique_filename", true
                )
            );

            Object secureUrl = uploadResult.get("secure_url");
            Object publicId = uploadResult.get("public_id");
            if (secureUrl == null || publicId == null) {
                Object err = uploadResult.get("error");
                String errMsg = String.valueOf(err != null ? err : uploadResult);
                if (isInvalidCloudName(errMsg)) {
                    return storageGuard.fallbackToLocalOrThrow(
                        errMsg,
                        () -> localImageStorageService.store(file)
                    );
                }
                throw new CloudinaryException("Upload Cloudinary thất bại: " + errMsg);
            }

            return new CloudinaryUploadResult(
                secureUrl.toString(),
                publicId.toString()
            );

        } catch (CloudinaryException e) {
            if (isInvalidCloudName(e.getMessage())) {
                return storageGuard.fallbackToLocalOrThrow(
                    e.getMessage(),
                    () -> localImageStorageService.store(file)
                );
            }
            throw e;
        } catch (IOException e) {
            throw new CloudinaryException("Không đọc được file ảnh: " + e.getMessage(), e);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            if (isInvalidCloudName(msg) || !cloudinaryConfig.isConfigured()) {
                return storageGuard.fallbackToLocalOrThrow(
                    msg,
                    () -> localImageStorageService.store(file)
                );
            }
            throw new CloudinaryException(
                "Upload ảnh thất bại: " + msg
                    + ". Kiểm tra CLOUDINARY_CLOUD_NAME / API_KEY / API_SECRET trên Railway "
                    + "(không dùng giá trị giả như SEDSP).",
                e
            );
        }
    }

    @Override
    public void deleteImage(String publicId) {
        if (publicId != null && publicId.startsWith("local/")) {
            localImageStorageService.deleteIfLocal(publicId);
            return;
        }
        if (!cloudinaryConfig.isConfigured()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new CloudinaryException("Không xóa được ảnh Cloudinary", e);
        }
    }

    @Override
    public void deleteImagesBulk(Collection<String> publicIds) {
        for (String id : publicIds) {
            if (id != null && id.startsWith("local/")) {
                localImageStorageService.deleteIfLocal(id);
            }
        }
        var remote = publicIds.stream()
            .filter(id -> id != null && !id.startsWith("local/"))
            .toList();
        if (remote.isEmpty() || !cloudinaryConfig.isConfigured()) {
            return;
        }
        try {
            cloudinary.api().deleteResources(remote, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new CloudinaryException("Không xóa hàng loạt ảnh Cloudinary", e);
        }
    }

    private static boolean isInvalidCloudName(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("invalid cloud_name")
            || lower.contains("unknown cloud_name")
            || lower.contains("cloud_name is invalid");
    }
}
