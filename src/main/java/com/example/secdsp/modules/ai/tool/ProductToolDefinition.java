package com.example.secdsp.modules.ai.tool;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;

import java.util.HashMap;
import java.util.Map;

public final class ProductToolDefinition {

    private ProductToolDefinition() {
    }

    public static FunctionDeclaration searchProducts() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put(
            "keyword",
            Schema.builder()
                .type("STRING")
                .description(
                    "Product name or search keyword. "
                        + "May be empty when filtering only by minPrice/maxPrice "
                        + "(e.g. user asks for anything under a budget)."
                )
                .build()
        );
        properties.put(
            "maxPrice",
            Schema.builder()
                .type("NUMBER")
                .description(
                    "Maximum selling price in VND inclusive. "
                        + "Example: 2000000 for \"dưới 2 triệu\"."
                )
                .build()
        );
        properties.put(
            "minPrice",
            Schema.builder()
                .type("NUMBER")
                .description(
                    "Minimum selling price in VND inclusive. "
                        + "Example: 10000000 for \"trên 10 triệu\"."
                )
                .build()
        );

        return FunctionDeclaration.builder()
            .name("search_products")
            .description("""
                Search products on the e-commerce platform.
                Use when the user wants to find, browse, or filter products
                by name/keyword and/or price range (budget).
                For budget questions like "điện thoại dưới 20 triệu", you MUST set
                maxPrice=20000000 (VND, inclusive) AND keyword "điện thoại".
                Never invent products — only report tool results. Never recommend a product
                whose price exceeds maxPrice.
                If the keyword is a phone request, do not return tablets (máy tính bảng, iPad, Galaxy Tab).
                """)
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(properties)
                    .build()
            )
            .build();
    }

    public static FunctionDeclaration getProductDetail() {
        return FunctionDeclaration.builder()
            .name("get_product_detail")
            .description("""
                Get detailed information about a specific product.
                Use this tool when the user asks about a product's
                price, description, stock, seller, images, or attributes.
                """)
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(
                        java.util.Map.of(
                            "productId",
                            Schema.builder()
                                .type("INTEGER")
                                .description("Product identifier")
                                .build()
                        )
                    )
                    .required(java.util.List.of("productId"))
                    .build()
            )
            .build();
    }
}
