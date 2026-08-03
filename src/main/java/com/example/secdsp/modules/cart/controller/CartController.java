package com.example.secdsp.modules.cart.controller;

import com.example.secdsp.common.api.BaseResponse;
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
    public ResponseEntity<BaseResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(
            BaseResponse.success(cartService.getMyCart())
        );
    }

    @PostMapping("/items")
    public ResponseEntity<BaseResponse<CartResponse>>
    addItem(@RequestBody @Valid AddCartItemRequest request) {

        return ResponseEntity.ok(
            BaseResponse.success(
                "Item added to cart",
                cartService.addItem(request)
            )
        );
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<BaseResponse<CartResponse>>
    updateItem(
        @PathVariable Long itemId,
        @RequestBody @Valid UpdateCartItemRequest request
    ) {

        return ResponseEntity.ok(
            BaseResponse.success(
                "Cart item updated",
                cartService.updateItem(itemId, request)
            )
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<BaseResponse<Void>>
    removeItem(@PathVariable Long itemId) {

        cartService.removeItem(itemId);

        return ResponseEntity.ok(
            BaseResponse.success("Item removed")
        );
    }

    @DeleteMapping
    public ResponseEntity<BaseResponse<Void>> clearCart() {

        cartService.clearCart();

        return ResponseEntity.ok(
            BaseResponse.success("Cart cleared")
        );
    }
}
