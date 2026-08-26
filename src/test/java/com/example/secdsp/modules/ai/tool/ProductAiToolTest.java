package com.example.secdsp.modules.ai.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductAiToolTest {

    @Test
    void galaxyTabIsNotAPhone() {
        assertTrue(ProductAiTool.looksLikeTablet("Samsung Galaxy Tab S9 Máy tính bảng"));
        assertFalse(ProductAiTool.looksLikePhone("Samsung Galaxy Tab S9 Máy tính bảng"));
        assertTrue(ProductAiTool.looksLikePhone("Điện thoại OnePlus 12 Điện thoại"));
        assertTrue(ProductAiTool.looksLikePhone("Samsung Galaxy S24"));
    }

    @Test
    void phoneBudgetQueryInfersPhoneDomainNotTablet() {
        assertEquals("điện thoại", ProductAiTool.parseDomainKeyword("điện thoại dưới 20 triệu"));
        assertEquals("máy tính bảng", ProductAiTool.parseDomainKeyword("máy tính bảng dưới 5 triệu"));
    }
}
