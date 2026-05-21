package com.acta.springserver.domain.user.entity;

import com.acta.springserver.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private boolean termsAgreed;

    @Column(nullable = false)
    private boolean privacyPolicyAgreed;

    @Column(nullable = false)
    private boolean deleted;

    @Column
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider socialProvider;

    @Column(length = 100)
    private String providerUserId;

    @Builder
    private User(
            String email,
            String password,
            String nickname,
            UserRole role,
            boolean emailVerified,
            boolean termsAgreed,
            boolean privacyPolicyAgreed,
            boolean deleted,
            LocalDateTime deletedAt,
            SocialProvider socialProvider,
            String providerUserId
    ) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.emailVerified = emailVerified;
        this.termsAgreed = termsAgreed;
        this.privacyPolicyAgreed = privacyPolicyAgreed;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.socialProvider = socialProvider;
        this.providerUserId = providerUserId;
    }

    public static User create(
            String email,
            String password,
            String nickname,
            boolean termsAgreed,
            boolean privacyPolicyAgreed
    ) {
        return User.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .role(UserRole.USER)
                .emailVerified(false)
                .termsAgreed(termsAgreed)
                .privacyPolicyAgreed(privacyPolicyAgreed)
                .deleted(false)
                .deletedAt(null)
                .socialProvider(SocialProvider.LOCAL)
                .providerUserId(null)
                .build();
    }

    public static User createSocialUser(
            String email,
            String nickname,
            SocialProvider socialProvider,
            String providerUserId
    ) {
        return User.builder()
                .email(resolveSocialEmail(email, socialProvider, providerUserId))
                .password(null)
                .nickname(nickname)
                .role(UserRole.USER)
                .emailVerified(true)
                .termsAgreed(true)
                .privacyPolicyAgreed(true)
                .deleted(false)
                .deletedAt(null)
                .socialProvider(socialProvider)
                .providerUserId(providerUserId)
                .build();
    }

    private static String resolveSocialEmail(
            String email,
            SocialProvider socialProvider,
            String providerUserId
    ) {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return "social_" + socialProvider.name().toLowerCase() + "_" + providerUserId;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}