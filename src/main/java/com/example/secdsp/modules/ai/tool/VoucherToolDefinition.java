package com.example.secdsp.modules.ai.tool;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;

import java.util.List;
import java.util.Map;

public final class VoucherToolDefinition {

    private VoucherToolDefinition() {
    }

    public static FunctionDeclaration listPublicVouchers() {

        return FunctionDeclaration.builder()
            .name("list_public_vouchers")
            .description(
                """
                Get publicly available vouchers on the e-commerce platform.

                Use this tool when the user asks:
                - what vouchers are available
                - available promotions
                - available discount codes
                - vouchers from a specific seller

                Do not invent voucher information.
                """
            )
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(
                        Map.of(
                            "sellerId",
                            Schema.builder()
                                .type("INTEGER")
                                .description(
                                    "Optional seller ID. " +
                                        "Use only when the user specifies a seller."
                                )
                                .build()
                        )
                    )
                    .build()
            )
            .build();
    }

    public static FunctionDeclaration validateVoucher() {

        return FunctionDeclaration.builder()
            .name("validate_voucher")
            .description(
                """
                Validate a voucher code for the current user's cart.

                Use this tool when the user asks whether a voucher
                can be used, whether a discount code is valid,
                or how much discount a voucher provides.

                Never guess voucher validity or discount amount.
                The backend result is the source of truth.
                """
            )
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(
                        Map.of(
                            "code",
                            Schema.builder()
                                .type("STRING")
                                .description(
                                    "Voucher code provided by the user."
                                )
                                .build(),

                            "productIds",
                            Schema.builder()
                                .type("ARRAY")
                                .items(
                                    Schema.builder()
                                        .type("INTEGER")
                                        .build()
                                )
                                .description(
                                    "Product IDs currently in the user's cart, " +
                                        "if known."
                                )
                                .build()
                        )
                    )
                    .required(
                        List.of("code")
                    )
                    .build()
            )
            .build();
    }
}