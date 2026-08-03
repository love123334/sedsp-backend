package com.example.secdsp.modules.email.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

@Entity
@Table(name = "email_otps")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 150)
    String email;

    @Column(nullable = false, length = 10)
    String otp;

    @Column(name = "expiry_time", nullable = false)
    OffsetDateTime expiryTime;

    @Column(name = "resend_count", nullable = false)
    int resendCount = 0;

    @Column(nullable = false)
    boolean verified = false;

    @Column(nullable = false)
    boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}