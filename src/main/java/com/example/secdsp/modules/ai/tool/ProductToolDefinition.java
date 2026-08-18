package com.example.secdsp.modules.ai.tool;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;

public final class ProductToolDefinition {

    private ProductToolDefinition() {
    }

    public static FunctionDeclaration searchProducts() {
        return FunctionDeclaration.builder()
            .name("search_products")
            .description("""
                Search products available on the e-commerce platform.
                Use this tool when the user asks to find, search, or recommend
                products based on a product name or keyword.
                """)
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(
                        java.util.Map.of(
                            "keyword",
                            Schema.builder()
                                .type("STRING")
                                .description("Product name or search keyword")
                                .build()
                        )
                    )
                    .required(java.util.List.of("keyword"))
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