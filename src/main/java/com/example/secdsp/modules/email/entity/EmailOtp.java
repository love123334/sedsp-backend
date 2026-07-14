package com.example.secdsp.modules.email.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_otps")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String email;

    String otp;

    LocalDateTime expiryTime;

    boolean used;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @Column(nullable = false)
    int resendCount;

    @Column(nullable = false)
    boolean verified;
}
