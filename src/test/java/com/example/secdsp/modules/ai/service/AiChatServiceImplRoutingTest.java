package com.example.secdsp.modules.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiChatServiceImplRoutingTest {

    @Test
    void groundedWhenFrontendSendsProductContext() {
        assertTrue(
            AiChatServiceImpl.alreadyGroundedByFrontend(
                "[CONTEXT SẢN PHẨM/SHOP — UI sẽ hiện card]\n- iPhone 15: 19.990.000đ"
            )
        );
        assertTrue(AiChatServiceImpl.alreadyGroundedByFrontend("- DSS: Holt-Winters 30 ngày"));
        assertFalse(AiChatServiceImpl.alreadyGroundedByFrontend("có điện thoại nào dưới 10 triệu"));
    }

    @Test
    void sellerRoleFromFrontendPrefix() {
        assertTrue(AiChatServiceImpl.isSellerOrManager("[Vai trò SEDSP: seller]\ntháng này bán thế nào"));
        assertTrue(AiChatServiceImpl.isSellerOrManager("[Vai trò SEDSP: manager]\nKPI"));
        assertFalse(AiChatServiceImpl.isSellerOrManager("mình muốn mua tai nghe"));
    }
}
