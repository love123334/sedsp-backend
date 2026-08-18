package com.example.secdsp.modules.ai.tool;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;

import java.util.Map;

public final class OrderToolDefinition {

    private OrderToolDefinition() {
    }

    public static FunctionDeclaration getMyOrders() {

        return FunctionDeclaration.builder()
            .name("get_my_orders")
            .description(
                "Get the authenticated customer's recent orders. " +
                    "Use this tool when the user asks about their orders, " +
                    "order history, recent purchases or order status."
            )
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(Map.of())
                    .build()
            )
            .build();
    }

    public static FunctionDeclaration getOrderDetail() {

        return FunctionDeclaration.builder()
            .name("get_order_detail")
            .description(
                "Get detailed information about a specific order " +
                    "including order status, products, quantities, prices, " +
                    "shipping address and payment information."
            )
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(
                        Map.of(
                            "orderId",
                            Schema.builder()
                                .type("INTEGER")
                                .description(
                                    "The ID of the order to retrieve."
                                )
                                .build()
                        )
                    )
                    .required(
                        java.util.List.of("orderId")
                    )
                    .build()
            )
            .build();
    }
}