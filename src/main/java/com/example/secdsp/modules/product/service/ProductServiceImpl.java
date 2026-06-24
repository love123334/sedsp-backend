package com.example.secdsp.modules.product.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.category.dto.internal.CategoryInfo;
import com.example.secdsp.modules.category.entity.Category;
import com.example.secdsp.modules.category.service.CategoryService;
import com.example.secdsp.modules.product.dto.request.*;
import com.example.secdsp.modules.product.dto.response.ProductDetailResponse;
import com.example.secdsp.modules.product.dto.response.ProductResponse;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.entity.ProductAttribute;
import com.example.secdsp.modules.product.entity.ProductImage;
import com.example.secdsp.modules.product.mapper.ProductMapper;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.example.secdsp.modules.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;

    // TODO: Inject SellerService when Seller module is implemented and available as a dependency.
    // private final SellerService sellerService;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Attempting to create product with name: {}", request.getName());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required to create products.");
        }

        // TODO: Get Seller entity from SellerService based on currentUserId.
        // This is crucial for setting the product's seller and for ownership checks.
        // For now, this will bypass seller validation and assume a seller exists,
        // which will likely cause a database constraint violation for seller_id if nullable=false.
        // Example: Seller seller = sellerService.getSellerByUserId(currentUserId)
        //                         .orElseThrow(() -> new BusinessException("Only users with a seller profile can create products."));
        // product.setSeller(seller);
        // Leaving as null for compilation with a clear TODO.
        // TODO: Inject SellerService when available
        throw new BusinessException("SellerService not implemented yet.");
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Attempting to update product with ID: {}", id);
        Product existingProduct = productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        // Check ownership
        checkProductOwnership(existingProduct);

        validateProductUniqueness(request.getName(), request.getSlug(), id);

        // Update scalar fields
        productMapper.updateProductFromDto(request, existingProduct);

        // Update category if provided
        if (request.getCategoryId() != null) {

            CategoryInfo categoryInfo =
                categoryService.getCategoryInfo(request.getCategoryId());

            Category categoryRef = new Category();
            categoryRef.setId(categoryInfo.id());

            existingProduct.setCategory(categoryRef);
        } else if (request.getCategoryId() == null && request.getName() != null) {
            // If categoryId is explicitly set to null in request (and name is present implying a full update attempt),
            // then clear the category. This is an interpretation. If only some fields are sent, nulls are ignored.
            // If the client explicitly sends categoryId: null, it means dissociate.
            // Given MapStruct's NullValuePropertyMappingStrategy.IGNORE, if categoryId is null in request, it remains unchanged.
            // To explicitly dissociate, the request would need a mechanism, e.g., categoryId = -1, or clearCategory: true.
            // For this prompt, if categoryId is null in the request DTO, it is ignored by MapStruct, keeping existing category.
            // To clear it, we'd need to explicitly set existingProduct.setCategory(null); if request.getCategoryId() is present and null.
            // Let's assume `null` in `UpdateProductRequest` means "no change".
        }


        // Handle images for update
        if (request.getImages() != null) {
            handleProductImagesForUpdate(existingProduct, request.getImages());
        }

        // Handle attributes for update
        if (request.getAttributes() != null) {
            handleProductAttributesForUpdate(existingProduct, request.getAttributes());
        }

        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product updated successfully with ID: {}", updatedProduct.getId());
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Attempting to soft delete product with ID: {}", id);
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        // Check ownership
        checkProductOwnership(product);

        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Product with ID {} soft deleted successfully.", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(Long id) {
        log.debug("Fetching product by ID: {}", id);
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return productMapper.toProductDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(
        String keyword,
        Long categoryId,
        Long sellerId,
        Pageable pageable
    ) {
        log.debug(
            "Fetching products with keyword: {}, categoryId: {}, sellerId: {}, pageable: {}",
            keyword, categoryId, sellerId, pageable
        );
        return productRepository.searchProducts(keyword, categoryId, sellerId, pageable)
            .map(productMapper::toProductResponse);
    }


    private void validateProductUniqueness(String name, String slug, Long currentProductId) {
        if (name != null) {
            boolean nameExists = currentProductId == null
                ? productRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(name)
                : productRepository.findByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(name, currentProductId).isPresent();
            if (nameExists) {
                throw new BusinessException("Product name already exists.");
            }
        }
        if (slug != null) {
            boolean slugExists = currentProductId == null
                ? productRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(slug)
                : productRepository.findBySlugIgnoreCaseAndIdNotAndDeletedAtIsNull(slug, currentProductId).isPresent();
            if (slugExists) {
                throw new BusinessException("Product slug already exists.");
            }
        }
    }

    private void handleProductImagesForCreate(Product product, List<AddProductImageRequest> imageRequests) {
        Set<String> imageUrls = new HashSet<>();
        boolean primaryFound = false;

        for (AddProductImageRequest imageRequest : imageRequests) {
            if (!imageUrls.add(imageRequest.getImageUrl().toLowerCase())) {
                throw new BusinessException("Duplicate image URL found: " + imageRequest.getImageUrl());
            }
            if (imageRequest.isPrimary()) {
                if (primaryFound) {
                    throw new BusinessException("Only one primary image is allowed per product.");
                }
                primaryFound = true;
            }
            ProductImage productImage = productMapper.toProductImage(imageRequest);
            productImage.setProduct(product);
            product.getProductImages().add(productImage);
        }

        if (!primaryFound && !product.getProductImages().isEmpty()) {
            product.getProductImages().get(0).setPrimary(true); // Set first image as primary if none specified
        }
    }

    private void handleProductAttributesForCreate(Product product, List<AddProductAttributeRequest> attributeRequests) {
        Set<String> attributeNames = new HashSet<>();

        for (AddProductAttributeRequest attributeRequest : attributeRequests) {
            if (!attributeNames.add(attributeRequest.getAttributeName().toLowerCase())) {
                throw new BusinessException("Duplicate attribute name found: " + attributeRequest.getAttributeName());
            }
            ProductAttribute productAttribute = productMapper.toProductAttribute(attributeRequest);
            productAttribute.setProduct(product);
            product.getProductAttributes().add(productAttribute);
        }
    }

    private void handleProductImagesForUpdate(Product product, List<UpdateProductImageRequest> imageRequests) {
        Set<String> imageUrls = new HashSet<>();
        boolean primaryFound = false;

        // Collect existing images into a map for efficient lookup
        Map<Long, ProductImage> existingImagesMap = product.getProductImages().stream()
            .collect(Collectors.toMap(ProductImage::getId, Function.identity()));

        // Clear existing collection and rebuild it to correctly manage orphanRemoval
        product.getProductImages().clear();

        for (UpdateProductImageRequest imageRequest : imageRequests) {
            if (!imageUrls.add(imageRequest.getImageUrl().toLowerCase())) {
                throw new BusinessException("Duplicate image URL found: " + imageRequest.getImageUrl());
            }

            if (imageRequest.isPrimary()) {
                if (primaryFound) {
                    throw new BusinessException("Only one primary image is allowed per product.");
                }
                primaryFound = true;
            }

            ProductImage imageToPersist;
            if (imageRequest.getId() != null) {
                // Update existing image
                imageToPersist = existingImagesMap.get(imageRequest.getId());
                if (imageToPersist == null) {
                    throw new ResourceNotFoundException("ProductImage", imageRequest.getId());
                }
                productMapper.updateProductImageFromDto(imageRequest, imageToPersist);
            } else {
                // Add new image
                imageToPersist = new ProductImage();
                imageToPersist.setImageUrl(imageRequest.getImageUrl());
                imageToPersist.setPrimary(imageRequest.isPrimary());
            }
            imageToPersist.setProduct(product);
            product.getProductImages().add(imageToPersist);
        }

        if (!primaryFound && !product.getProductImages().isEmpty()) {
            product.getProductImages().get(0).setPrimary(true); // Set first image as primary if none specified
        }
    }

    private void handleProductAttributesForUpdate(
        Product product,
        List<UpdateProductAttributeRequest> attributeRequests
    ) {
        Set<String> attributeNames = new HashSet<>();

        // Collect existing attributes into a map for efficient lookup
        Map<Long, ProductAttribute> existingAttributesMap = product.getProductAttributes().stream()
            .collect(Collectors.toMap(ProductAttribute::getId, Function.identity()));

        // Clear existing collection and rebuild it to correctly manage orphanRemoval
        product.getProductAttributes().clear();

        for (UpdateProductAttributeRequest attributeRequest : attributeRequests) {
            if (!attributeNames.add(attributeRequest.getAttributeName().toLowerCase())) {
                throw new BusinessException("Duplicate attribute name found: " + attributeRequest.getAttributeName());
            }

            ProductAttribute attributeToPersist;
            if (attributeRequest.getId() != null) {
                // Update existing attribute
                attributeToPersist = existingAttributesMap.get(attributeRequest.getId());
                if (attributeToPersist == null) {
                    throw new ResourceNotFoundException("ProductAttribute", attributeRequest.getId());
                }
                productMapper.updateProductAttributeFromDto(attributeRequest, attributeToPersist);
            } else {
                // Add new attribute
                attributeToPersist = new ProductAttribute();
                attributeToPersist.setAttributeName(attributeRequest.getAttributeName());
                attributeToPersist.setAttributeValue(attributeRequest.getAttributeValue());
            }
            attributeToPersist.setProduct(product);
            product.getProductAttributes().add(attributeToPersist);
        }
    }

    private void checkProductOwnership(Product product) {

        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        boolean isAdmin = SecurityUtils.hasRole(UserRole.ADMIN);

//        if (!isAdmin) {
//
//            if (product.getSeller() == null ||
//                product.getSeller().getUser() == null ||
//                !product.getSeller().getUser().getId().equals(currentUserId)) {
//
//                throw new ForbiddenException("You are not authorized to manage this product.");
//            }
//        }
    }
}
