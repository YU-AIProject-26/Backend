package com.acta.springserver.domain.auth.entity;

import com.acta.springserver.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailVerificationPurpose purpose;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    private EmailVerification(
            String email,
            String code,
            EmailVerificationPurpose purpose,
            boolean verified,
            LocalDateTime expiredAt
    ) {
        this.email = email;
        this.code = code;
        this.purpose = purpose;
        this.verified = verified;
        this.expiredAt = expiredAt;
    }

    public static EmailVerification create(
            String email,
            String code,
            EmailVerificationPurpose purpose,
            LocalDateTime expiredAt
    ) {
        return EmailVerification.builder()
                .email(email)
                .code(code)
                .purpose(purpose)
                .verified(false)
                .expiredAt(expiredAt)
                .build();
    }

    public void markVerified() {
        this.verified = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiredAt);
    }
}