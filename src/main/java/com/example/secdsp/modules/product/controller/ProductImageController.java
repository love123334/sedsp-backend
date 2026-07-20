package com.example.secdsp.modules.product.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.infrastructure.cloudinary.CloudinaryService;
import com.example.secdsp.modules.product.dto.response.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products/images")
@RequiredArgsConstructor
public class ProductImageController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<ApiResponse<CloudinaryUploadResult>>  uploadImage(
        @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new BusinessException("File must not be empty");
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("File size must be <= 2MB");
        }

        CloudinaryUploadResult imageUrl = cloudinaryService.uploadImage(file);

        return ResponseEntity.ok(
            ApiResponse.success("Upload successful", imageUrl)
        );
    }
}