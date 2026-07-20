package com.example.secdsp.infrastructure.cloudinary;

import com.example.secdsp.modules.product.dto.response.CloudinaryUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

public interface CloudinaryService {

    CloudinaryUploadResult uploadImage(MultipartFile file);

    void deleteImage(String publicId);

    void deleteImagesBulk(Collection<String> publicIds);
}