package com.example.secdsp.modules.product.service;

import com.example.secdsp.common.exception.BusinessException;
import com.example.secdsp.common.exception.ResourceNotFoundException;
import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.category.dto.internal.CategoryInfo;
import com.example.secdsp.modules.category.entity.Category;
import com.example.secdsp.modules.category.service.CategoryService;
import com.example.secdsp.modules.product.dto.internal.LowStockProductInfo;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.dto.internal.ProductSummaryInfo;
import com.example.secdsp.modules.product.dto.request.*;
import com.example.secdsp.modules.product.dto.response.PriceHistoryResponse;
import com.example.secdsp.modules.product.dto.response.ProductDetailResponse;
import com.example.secdsp.modules.product.dto.response.ProductResponse;
import com.example.secdsp.modules.product.entity.*;
import com.example.secdsp.modules.product.mapper.PriceHistoryMapper;
import com.example.secdsp.modules.product.mapper.ProductMapper;
import com.example.secdsp.modules.product.repository.PriceHistoryRepository;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.example.secdsp.modules.user.dto.internal.UserInfo;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import com.example.secdsp.modules.user.entity.UserStatus;
import com.example.secdsp.modules.user.service.UserService;
import com.github.slugify.Slugify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductMapper productMapper;
    private final PriceHistoryMapper priceHistoryMapper;
    private final CategoryService categoryService;
    private final UserService userService;
    private final Slugify slugify;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {

        log.info("Attempting to create product with name: {}", request.getName());

        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId == null) {
            throw new UnauthorizedException(
                "Authentication required to create products."
            );
        }

        UserInfo userInfo = userService.getUserInfo(currentUserId);

        if (userInfo.status() != UserStatus.ACTIVE) {
            throw new BusinessException("User account is not active.");
        }

        Product product = productMapper.toEntity(request);

        product.setSlug(generateUniqueSlug(request.getName(), null));

        User sellerRef = new User();
        sellerRef.setId(userInfo.id());
        product.setSeller(sellerRef);

        if (request.getCategoryId() != null) {

            CategoryInfo categoryInfo =
                categoryService.getCategoryInfo(request.getCategoryId());

            Category categoryRef = new Category();
            categoryRef.setId(categoryInfo.id());

            product.setCategory(categoryRef);
        }

        if (request.getImages() != null) {
            handleProductImagesForCreate(product, request.getImages());
        }

        if (request.getAttributes() != null) {
            handleProductAttributesForCreate(product, request.getAttributes());
        }

        Product saved = productRepository.save(product);
        log.info("Product created successfully with ID: {}", saved.getId());
        return productMapper.toProductResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {

        log.info("Attempting to update product with ID: {}", id);

        Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        checkProductOwnership(existingProduct);

        if (request.getPrice() != null) {

            if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(
                    "Giá sản phẩm phải lớn hơn 0."
                );
            }

            BigDecimal oldPrice = existingProduct.getPrice();
            BigDecimal newPrice = request.getPrice();

            if (oldPrice.compareTo(newPrice) != 0) {

                PriceHistory history = PriceHistory.builder()
                    .product(existingProduct)
                    .oldPrice(oldPrice)
                    .newPrice(newPrice)
                    .changedBy(buildCurrentUserRef())
                    .build();

                priceHistoryRepository.save(history);

                existingProduct.setPrice(newPrice);
            }
        }

        // ✅ Slug update nếu name đổi
        if (request.getName() != null &&
            !request.getName().equals(existingProduct.getName())) {

            existingProduct.setSlug(
                generateUniqueSlug(request.getName(), id)
            );
        }

        productMapper.updateProductFromDto(request, existingProduct);

        if (request.getCategoryId() != null) {
            CategoryInfo categoryInfo =
                categoryService.getCategoryInfo(request.getCategoryId());

            Category categoryRef = new Category();
            categoryRef.setId(categoryInfo.id());

            existingProduct.setCategory(categoryRef);
        }

        Product updatedProduct = productRepository.save(existingProduct);

        log.info("Product updated successfully with ID: {}", updatedProduct.getId());

        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {

        log.info("Attempting to delete product with ID: {}", id);

        Product product = productRepository.findById(id)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Product", id));

        checkProductOwnership(product);

        productRepository.delete(product);

        log.info("Product {} deleted successfully.", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(Long id) {
        log.debug("Fetching product by ID: {}", id);
        Product product = productRepository.findById(id)
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

    @Override
    @Transactional(readOnly = true)
    public ProductInfo getProductInfo(Long id) {

        Product product = productRepository.findById(id)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Product", id));

        return new ProductInfo(
            product.getId(),
            product.getSeller() != null
                ? product.getSeller().getId()
                : null,
            product.getName(),
            product.getPrice(),
            product.getStatus()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getPriceHistory(Long productId) {

        Product product = productRepository.findById(productId)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Product", productId));

        checkProductOwnership(product);

        List<PriceHistory> histories =
            priceHistoryRepository
                .findByProduct_IdOrderByChangedAtDesc(productId);

        return priceHistoryMapper.toResponse(histories);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSummaryInfo getSellerProductSummary(Long sellerId) {

        long total = productRepository.countBySeller_Id(sellerId);
        long active = productRepository
            .countBySeller_IdAndStatus(
                sellerId,
                ProductStatus.ACTIVE
            );

        return ProductSummaryInfo.builder()
            .totalProducts(total)
            .activeProducts(active)
            .build();
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

        if (SecurityUtils.hasRole(UserRole.ADMIN)) {
            return;
        }

        if (product.getSeller() == null ||
            !product.getSeller().getId().equals(currentUserId)) {

            throw new UnauthorizedException(
                "You are not allowed to manage this product."
            );
        }
    }

    private String generateUniqueSlug(String name, Long currentId) {

        String baseSlug = slugify.slugify(name);
        String slug = baseSlug;
        int counter = 1;

        while (true) {

            boolean exists = (currentId == null)
                ? productRepository.existsBySlugIgnoreCase(slug)
                : productRepository
                .findBySlugIgnoreCaseAndIdNot(slug, currentId)
                .isPresent();

            if (!exists) {
                return slug;
            }

            slug = baseSlug + "-" + counter++;
        }
    }

    private User buildCurrentUserRef() {

        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required.");
        }

        User user = new User();
        user.setId(currentUserId);

        return user;
    }
}
