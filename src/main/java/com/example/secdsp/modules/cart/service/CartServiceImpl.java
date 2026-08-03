package com.example.secdsp.modules.cart.service;

import com.example.secdsp.common.exception.*;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.cart.dto.request.AddCartItemRequest;
import com.example.secdsp.modules.cart.dto.request.UpdateCartItemRequest;
import com.example.secdsp.modules.cart.dto.response.CartItemResponse;
import com.example.secdsp.modules.cart.dto.response.CartResponse;
import com.example.secdsp.modules.cart.entity.Cart;
import com.example.secdsp.modules.cart.entity.CartItem;
import com.example.secdsp.modules.cart.mapper.CartItemMapper;
import com.example.secdsp.modules.cart.mapper.CartMapper;
import com.example.secdsp.modules.cart.repository.CartItemRepository;
import com.example.secdsp.modules.cart.repository.CartRepository;
import com.example.secdsp.modules.inventory.dto.response.InventoryResponse;
import com.example.secdsp.modules.inventory.service.InventoryService;
import com.example.secdsp.modules.product.dto.internal.ProductInfo;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.entity.ProductStatus;
import com.example.secdsp.modules.product.service.ProductService;
import com.example.secdsp.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;

    private final ProductService productService;
    private final InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getMyCart() {

        Long userId = getCurrentUserId();

        Cart cart = getOrCreateCart(userId);

        log.debug("Fetching cart for user {}", userId);

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(AddCartItemRequest request) {

        Long userId = getCurrentUserId();

        log.info(
            "User {} adding product {} to cart",
            userId, request.getProductId()
        );

        ProductInfo product =
            productService.getProductInfo(request.getProductId());

        if (product.status() != ProductStatus.ACTIVE) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "Product is not available."
            );
        }

        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository
            .findByCart_IdAndProduct_Id(cart.getId(), product.id())
            .orElse(null);

        int currentQuantity = (item == null ? 0 : item.getQuantity());
        int newQuantity = currentQuantity + request.getQuantity();

        validateStock(product.id(), newQuantity);

        if (item != null) {
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            item = new CartItem();
            item.setCart(cart);

            Product productRef = new Product();
            productRef.setId(product.id());
            item.setProduct(productRef);

            item.setQuantity(request.getQuantity());

            cartItemRepository.save(item);
        }

        log.info("Cart successfully updated for user {}", userId);

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(
        Long itemId,
        UpdateCartItemRequest request
    ) {

        Long userId = getCurrentUserId();

        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() ->
                             new ResourceNotFoundException("CartItem", itemId));

        validateCartOwner(item, userId);

        if (request.getQuantity() <= 0) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "Quantity must be greater than 0."
            );
        }

        validateStock(item.getProduct().getId(), request.getQuantity());

        item.setQuantity(request.getQuantity());

        return buildCartResponse(item.getCart());
    }

    @Override
    @Transactional
    public void removeItem(Long itemId) {

        Long userId = getCurrentUserId();

        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() ->
                             new ResourceNotFoundException("CartItem", itemId));

        validateCartOwner(item, userId);

        log.info("User {} removing cart item {}", userId, itemId);

        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clearCart() {

        Long userId = getCurrentUserId();

        Cart cart = getOrCreateCart(userId);

        log.info("User {} clearing cart {}", userId, cart.getId());

        cartItemRepository.deleteAllByCart_Id(cart.getId());
    }

    private CartResponse buildCartResponse(Cart cart) {

        List<CartItem> items =
            cartItemRepository.findByCart_Id(cart.getId());

        List<CartItemResponse> responses =
            items.stream().map(item -> {

                CartItemResponse base =
                    cartItemMapper.toResponse(item);

                return CartItemResponse.builder()
                    .productId(base.getProductId())
                    .productName(base.getProductName())
                    .price(base.getPrice())
                    .quantity(item.getQuantity())
                    .totalPrice(
                        base.getPrice()
                            .multiply(
                                BigDecimal.valueOf(
                                    item.getQuantity())))
                    .build();
            }).toList();

        BigDecimal total =
            responses.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
            .cartId(cart.getId())
            .userId(cart.getUser().getId())
            .items(responses)
            .totalAmount(total)
            .build();
    }

    private Cart getOrCreateCart(Long userId) {

        return cartRepository.findByUser_Id(userId)
            .orElseGet(() -> {
                Cart cart = new Cart();
                User user = new User();
                user.setId(userId);
                cart.setUser(user);
                return cartRepository.save(cart);
            });
    }

    private Long getCurrentUserId() {

        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new UnauthorizedException();
        }

        return userId;
    }

    private void validateStock(Long productId, int quantity) {

        InventoryResponse inventory =
            inventoryService.getInventoryByProductId(productId);

        if (inventory.getAvailableQuantity() < quantity) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ERROR,
                "Insufficient stock."
            );
        }
    }

    private void validateCartOwner(
        CartItem item,
        Long userId
    ) {

        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new ForbiddenException(
                "You cannot access this cart item.");
        }
    }
}
