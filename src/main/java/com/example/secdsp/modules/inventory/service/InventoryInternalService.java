package com.example.secdsp.modules.inventory.service;

public interface InventoryInternalService {

    void reserveForOrder(Long productId, int quantity);

    void releaseForCancel(Long productId, int quantity);
}