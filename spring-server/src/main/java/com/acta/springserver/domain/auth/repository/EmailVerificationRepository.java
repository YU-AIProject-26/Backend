package com.acta.springserver.domain.auth.repository;

import com.acta.springserver.domain.auth.entity.EmailVerification;
import com.acta.springserver.domain.auth.entity.EmailVerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email,
            EmailVerificationPurpose purpose
    );

    void deleteByEmailAndPurpose(String email, EmailVerificationPurpose purpose);
}