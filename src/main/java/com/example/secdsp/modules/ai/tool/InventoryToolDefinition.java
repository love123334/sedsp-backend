package com.example.secdsp.modules.ai.tool;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;

import java.util.List;
import java.util.Map;

public final class InventoryToolDefinition {

    private InventoryToolDefinition() {
    }

    public static FunctionDeclaration getInventory() {

        return FunctionDeclaration.builder()
            .name("get_inventory")
            .description(
                "Get inventory information for a specific product. " +
                    "Use this when the user asks about product stock, " +
                    "available quantity, reserved quantity, or inventory status."
            )
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(
                        Map.of(
                            "productId",
                            Schema.builder()
                                .type("INTEGER")
                                .description(
                                    "The product ID whose inventory should be retrieved."
                                )
                                .build()
                        )
                    )
                    .required(List.of("productId"))
                    .build()
            )
            .build();
    }

    public static FunctionDeclaration getInventorySummary() {

        return FunctionDeclaration.builder()
            .name("get_inventory_summary")
            .description(
                "Get inventory summary for the currently authenticated seller, " +
                    "including the number of low-stock products and out-of-stock products."
            )
            .build();
    }

    public static FunctionDeclaration getLowStockProducts() {

        return FunctionDeclaration.builder()
            .name("get_low_stock_products")
            .description(
                "Get products belonging to the currently authenticated seller " +
                    "that have low stock."
            )
            .build();
    }
}