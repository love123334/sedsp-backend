package com.example.secdsp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.order")
@Getter
@Setter
public class OrderProperties {

    /** Unpaid VNPay/MoMo orders older than this are auto-cancelled. */
    private int paymentTimeoutMinutes = 15;
}
