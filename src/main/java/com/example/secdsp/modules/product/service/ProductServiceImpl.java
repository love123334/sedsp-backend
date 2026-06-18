package com.example.secdsp.modules.product.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ForbiddenException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.brand.entity.Brand;
import com.example.secdsp.modules.brand.service.BrandService;
import com.example.secdsp.modules.category.entity.Category;
import com.example.secdsp.modules.category.service.CategoryService;
import com.example.secdsp.modules.product.dto.request.*;
import com.example.secdsp.modules.product.dto.response.ProductDetailResponse;
import com.example.secdsp.modules.product.dto.response.ProductResponse;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.entity.ProductAttribute;
import com.example.secdsp.modules.product.entity.ProductImage;
import com.example.secdsp.modules.product.mapper.ProductMapper;
import com.example.secdsp.modules.product.repository.ProductAttributeRepository;
import com.example.secdsp.modules.product.repository.ProductImageRepository;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.example.secdsp.modules.seller.entity.Seller;
import com.example.secdsp.modules.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final BrandService brandService;

    // Placeholder for actual SellerService dependency if it were present.
    // For this prompt, direct injection of SellerService is not possible as it's not created.
    // In a real application, this would be injected and used.
    // TODO: Inject SellerService when Seller module is implemented.
    // private final SellerService sellerService;
    private final ProductImageRepository productImageRepository; // For managing images directly
    private final ProductAttributeRepository productAttributeRepository; // For managing attributes directly

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Attempting to create product with name: {}", request.getName());
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required to create products.");
        }

        // TODO: Get Seller entity from SellerService based on currentUserId
        // Example: Seller seller = sellerService.getSellerByUserId(currentUserId)
        //                         .orElseThrow(() -> new BusinessException("Only users with a seller profile can create products."));
        // For now, we'll assign null and rely on the database schema for non-null constraints,
        // which will fail if seller_id is nullable=false, but this is a placeholder.
        Seller seller = new Seller(); // Placeholder for compilation, will cause runtime error if not handled
        seller.setId(1L); // Assign a dummy ID for compilation
        // END TODO

        validateProductUniqueness(request.getName(), request.getSlug(), null);

        Product product = productMapper.toEntity(request);
        product.setSeller(seller); // Assign seller

        if (request.getCategoryId() != null) {
            Category category = categoryService.findEntityById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getBrandId() != null) {
            Brand brand = brandService.findEntityById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand", request.getBrandId()));
            product.setBrand(brand);
        }

        // Handle images for creation
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            handleProductImagesForCreate(product, request.getImages());
        }

        // Handle attributes for creation
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            handleProductAttributesForCreate(product, request.getAttributes());
        }

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());
        return productMapper.toProductResponse(savedProduct);
    }

    @Override
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
            Category category = categoryService.findEntityById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
            existingProduct.setCategory(category);
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


        // Update brand if provided
        if (request.getBrandId() != null) {
            Brand brand = brandService.findEntityById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand", request.getBrandId()));
            existingProduct.setBrand(brand);
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
        Long brandId,
        Long sellerId,
        Pageable pageable
    ) {
        log.debug(
            "Fetching products with keyword: {}, categoryId: {}, brandId: {}, sellerId: {}, pageable: {}",
            keyword, categoryId, brandId, sellerId, pageable
        );
        return productRepository.searchProducts(keyword, categoryId, brandId, sellerId, pageable)
            .map(productMapper::toProductResponse);
    }

    private void validateProductUniqueness(
        String name,
        String slug,
        Long id
    ) {

        if (id == null) { // CREATE

            if (name != null &&
                productRepository
                    .existsByNameIgnoreCaseAndDeletedAtIsNull(name)) {
                throw new BusinessException("Product name already exists");
            }

            if (slug != null &&
                productRepository
                    .existsBySlugIgnoreCaseAndDeletedAtIsNull(slug)) {
                throw new BusinessException("Product slug already exists");
            }

        } else { // UPDATE

            if (name != null) {
                productRepository
                    .findByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(name, id)
                    .ifPresent(p -> {
                        throw new BusinessException("Product name already exists");
                    });
            }

            if (slug != null) {
                productRepository
                    .findBySlugIgnoreCaseAndIdNotAndDeletedAtIsNull(slug, id)
                    .ifPresent(p -> {
                        throw new BusinessException("Product slug already exists");
                    });
            }
        }
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
        Set<Long> requestImageIds = imageRequests.stream()
            .map(UpdateProductImageRequest::getId)
            .collect(Collectors.toSet());

        // Remove images not in the update request
        product.getProductImages().removeIf(existingImage -> !requestImageIds.contains(existingImage.getId()));

        Set<String> imageUrls = new HashSet<>();
        boolean primaryFound = false;

        // Add/Update images
        for (UpdateProductImageRequest imageRequest : imageRequests) {
            if (imageRequest.getId() != null) {
                // Update existing image
                ProductImage existingImage = product.getProductImages().stream()
                    .filter(img -> img.getId().equals(imageRequest.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("ProductImage", imageRequest.getId()));

                if (!imageUrls.add(imageRequest.getImageUrl().toLowerCase())) {
                    throw new BusinessException("Duplicate image URL found: " + imageRequest.getImageUrl());
                }

                productMapper.updateProductImageFromDto(imageRequest, existingImage);
                if (imageRequest.isPrimary()) {
                    if (primaryFound) {
                        throw new BusinessException("Only one primary image is allowed per product.");
                    }
                    primaryFound = true;
                }
            } else {
                // Add new image
                if (!imageUrls.add(imageRequest.getImageUrl().toLowerCase())) {
                    throw new BusinessException("Duplicate image URL found: " + imageRequest.getImageUrl());
                }
                if (imageRequest.isPrimary()) {
                    if (primaryFound) {
                        throw new BusinessException("Only one primary image is allowed per product.");
                    }
                    primaryFound = true;
                }
                ProductImage newImage = new ProductImage();
                newImage.setImageUrl(imageRequest.getImageUrl());
                newImage.setPrimary(imageRequest.isPrimary());
                newImage.setProduct(product);
                product.getProductImages().add(newImage);
            }
        }

        if (!primaryFound && !product.getProductImages().isEmpty()) {
            product.getProductImages().get(0).setPrimary(true);
        }
    }

    private void handleProductAttributesForUpdate(Product product, List<UpdateProductAttributeRequest> attributeRequests) {
        Set<Long> requestAttributeIds = attributeRequests.stream()
            .map(UpdateProductAttributeRequest::getId)
            .collect(Collectors.toSet());

        // Remove attributes not in the update request
        product.getProductAttributes().removeIf(existingAttribute -> !requestAttributeIds.contains(existingAttribute.getId()));

        Set<String> attributeNames = new HashSet<>();

        // Add/Update attributes
        for (UpdateProductAttributeRequest attributeRequest : attributeRequests) {
            if (attributeRequest.getId() != null) {
                // Update existing attribute
                ProductAttribute existingAttribute = product.getProductAttributes().stream()
                    .filter(attr -> attr.getId().equals(attributeRequest.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("ProductAttribute", attributeRequest.getId()));

                if (!attributeNames.add(attributeRequest.getAttributeName().toLowerCase())) {
                    throw new BusinessException("Duplicate attribute name found: " + attributeRequest.getAttributeName());
                }

                productMapper.updateProductAttributeFromDto(attributeRequest, existingAttribute);
            } else {
                // Add new attribute
                if (!attributeNames.add(attributeRequest.getAttributeName().toLowerCase())) {
                    throw new BusinessException("Duplicate attribute name found: " + attributeRequest.getAttributeName());
                }
                ProductAttribute newAttribute = new ProductAttribute();
                newAttribute.setAttributeName(attributeRequest.getAttributeName());
                newAttribute.setAttributeValue(attributeRequest.getAttributeValue());
                newAttribute.setProduct(product);
                product.getProductAttributes().add(newAttribute);
            }
        }
    }

    private void checkProductOwnership(Product product) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        // Check if the current user is an ADMIN
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_" + UserRole.ADMIN.name()));

        if (!isAdmin) {
            // If not an ADMIN, check if the current user is the product's seller
            if (product.getSeller() == null || product.getSeller().getUser() == null || !product.getSeller().getUser().getId().equals(currentUserId)) {
                throw new ForbiddenException("You are not authorized to manage this product.");
            }
        }
    }
}
