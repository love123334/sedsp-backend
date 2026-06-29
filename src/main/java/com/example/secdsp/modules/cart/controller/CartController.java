package com.example.secdsp.modules.cart.controller;

import com.example.secdsp.common.api.ApiResponse;
import com.example.secdsp.modules.cart.dto.request.AddCartItemRequest;
import com.example.secdsp.modules.cart.dto.request.UpdateCartItemRequest;
import com.example.secdsp.modules.cart.dto.response.CartResponse;
import com.example.secdsp.modules.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(
            ApiResponse.success(cartService.getMyCart())
        );
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>>
    addItem(@RequestBody @Valid AddCartItemRequest request) {

        return ResponseEntity.ok(
            ApiResponse.success(
                "Item added to cart",
                cartService.addItem(request)
            )
        );
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>>
    updateItem(
        @PathVariable Long itemId,
        @RequestBody @Valid UpdateCartItemRequest request
    ) {

        return ResponseEntity.ok(
            ApiResponse.success(
                "Cart item updated",
                cartService.updateItem(itemId, request)
            )
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>>
    removeItem(@PathVariable Long itemId) {

        cartService.removeItem(itemId);

        return ResponseEntity.ok(
            ApiResponse.success("Item removed")
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart() {

        cartService.clearCart();

        return ResponseEntity.ok(
            ApiResponse.success("Cart cleared")
        );
    }
}
