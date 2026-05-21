package com.acta.springserver.domain.user.entity;

import com.acta.springserver.common.entity.BaseEntity;
import jakarta.persistence.*;
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

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean emailVerified;

    @Builder
    private User(String email, String password, String nickname, UserRole role, boolean emailVerified) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.emailVerified = emailVerified;
    }

    public static User create(String email, String password, String nickname) {
        return User.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .role(UserRole.USER)
                .emailVerified(false)
                .build();
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }
}