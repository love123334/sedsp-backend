package com.example.secdsp.modules.user.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = """
        Available values:
        - CUSTOMER : Customer account.
        - SELLER : Seller account.
        - MANAGER : Manager account.
        - ADMIN : Administrator account.
        """,
    implementation = UserRole.class
)
public enum UserRole {
    CUSTOMER,
    SELLER,
    MANAGER,
    ADMIN
}