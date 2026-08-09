package com.example.secdsp.modules.order.support;

public final class MomoTransferSupport {

    private MomoTransferSupport() {}

    public static String transferNote(Long orderId) {
        return "SEDSP DH#" + orderId;
    }
}
