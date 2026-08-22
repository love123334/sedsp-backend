package com.example.secdsp.modules.cart.repository.projection;

/** Product + qty in a cart for voucher validation. */
public interface CartProductQtyRow {

    Long getProductId();

    Integer getQuantity();
}
