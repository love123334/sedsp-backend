package com.example.secdsp.modules.email.repository;

import com.example.secdsp.modules.email.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByEmailOrderByIdDesc(String email);
}