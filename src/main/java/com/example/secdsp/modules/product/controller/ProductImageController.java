package com.example.secdsp.modules.product.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.infrastructure.cloudinary.CloudinaryService;
import com.example.secdsp.modules.product.dto.response.CloudinaryUploadResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products/images")
@RequiredArgsConstructor
@Tag(
    name = "Product Images",
    description = "APIs for uploading product images"
)
public class ProductImageController {

    private final CloudinaryService cloudinaryService;

    @Operation(
        summary = "Upload product image",
        description = """
            Upload an image to Cloudinary.
            
            Maximum file size: 2 MB.
            
            Supported image formats depend on the Cloudinary configuration.
            
            Requires ADMIN or SELLER role.
            """
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Upload successful"),
        @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    @PostMapping(
        value = "/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<BaseResponse<CloudinaryUploadResult>> uploadImage(

        @Parameter(
            description = "Image file to upload"
        )
        @RequestPart("file") MultipartFile file
    ) {

        if (file.isEmpty()) {
            throw new BusinessException("File must not be empty");
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("File size must be <= 2MB");
        }

        CloudinaryUploadResult imageUrl =
            cloudinaryService.uploadImage(file);

        return ResponseEntity.ok(
            BaseResponse.success(
                "Upload successful",
                imageUrl
            )
        );
    }
}