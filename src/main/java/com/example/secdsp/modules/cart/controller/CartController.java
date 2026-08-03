package com.example.secdsp.modules.cart.controller;

import com.example.secdsp.common.api.BaseResponse;
import com.example.secdsp.modules.cart.dto.request.AddCartItemRequest;
import com.example.secdsp.modules.cart.dto.request.UpdateCartItemRequest;
import com.example.secdsp.modules.cart.dto.response.CartResponse;
import com.example.secdsp.modules.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(
    name = "Shopping Cart",
    description = "APIs for managing the authenticated user's shopping cart"
)
@SecurityRequirement(name = "Bearer Authentication")
public class CartController {

    private final CartService cartService;

    @Operation(
        summary = "Get current cart",
        description = "Retrieve the shopping cart of the currently authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @GetMapping
    public ResponseEntity<BaseResponse<CartResponse>> getCart() {

        return ResponseEntity.ok(
            BaseResponse.success(cartService.getMyCart())
        );
    }

    @Operation(
        summary = "Add item to cart",
        description = "Add a product to the current user's shopping cart. If the product already exists in the cart, its quantity will be updated."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item added to cart"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    @PostMapping("/items")
    public ResponseEntity<BaseResponse<CartResponse>> addItem(
        @RequestBody @Valid AddCartItemRequest request
    ) {

        return ResponseEntity.ok(
            BaseResponse.success(
                "Item added to cart",
                cartService.addItem(request)
            )
        );
    }

    @Operation(
        summary = "Update cart item",
        description = "Update the quantity of an existing cart item."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cart item updated"),
        @ApiResponse(responseCode = "400", description = "Invalid quantity", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Cart item not found", content = @Content)
    })
    @PutMapping("/items/{itemId}")
    public ResponseEntity<BaseResponse<CartResponse>> updateItem(

        @Parameter(
            description = "Cart item identifier",
            example = "15"
        )
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

    @Operation(
        summary = "Remove cart item",
        description = "Remove a specific item from the shopping cart."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item removed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "404", description = "Cart item not found", content = @Content)
    })
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<BaseResponse<Void>> removeItem(

        @Parameter(
            description = "Cart item identifier",
            example = "15"
        )
        @PathVariable Long itemId
    ) {

        cartService.removeItem(itemId);

        return ResponseEntity.ok(
            BaseResponse.success("Item removed")
        );
    }

    @Operation(
        summary = "Clear shopping cart",
        description = "Remove all items from the current user's shopping cart."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cart cleared"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @DeleteMapping
    public ResponseEntity<BaseResponse<Void>> clearCart() {

        cartService.clearCart();

        return ResponseEntity.ok(
            BaseResponse.success("Cart cleared")
        );
    }

}