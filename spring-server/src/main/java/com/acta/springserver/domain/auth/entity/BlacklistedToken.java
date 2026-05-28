package com.acta.springserver.domain.auth.entity;

import com.acta.springserver.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlacklistedToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    private BlacklistedToken(String token, LocalDateTime expiredAt) {
        this.token = token;
        this.expiredAt = expiredAt;
    }

    public static BlacklistedToken create(String token, LocalDateTime expiredAt) {
        return BlacklistedToken.builder()
                .token(token)
                .expiredAt(expiredAt)
                .build();
    }
}