package com.example.secdsp.infrastructure.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.secdsp.modules.product.dto.response.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public CloudinaryUploadResult uploadImage(MultipartFile file) {
        try {
            // Keep upload options simple — invalid "transformation" strings break Cloudinary uploads
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
                throw new RuntimeException(
                    "Cloudinary upload failed: " + (err != null ? err : uploadResult)
                );
            }

            return new CloudinaryUploadResult(
                secureUrl.toString(),
                publicId.toString()
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image", e);
        }
    }

    @Override
    public void deleteImagesBulk(Collection<String> publicIds) {

        try {

            cloudinary.api().deleteResources(
                publicIds,
                ObjectUtils.emptyMap()
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to bulk delete images", e);
        }
    }
}