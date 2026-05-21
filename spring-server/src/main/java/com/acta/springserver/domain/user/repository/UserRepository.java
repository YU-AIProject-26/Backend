package com.acta.springserver.domain.user.repository;

import com.acta.springserver.domain.user.entity.SocialProvider;
import com.acta.springserver.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByNicknameAndDeletedFalse(String nickname);

    Optional<User> findByEmailAndDeletedFalse(String email);

    Optional<User> findByIdAndDeletedFalse(Long id);

    Optional<User> findByNicknameAndDeletedFalse(String nickname);

    Optional<User> findBySocialProviderAndProviderUserIdAndDeletedFalse(
            SocialProvider socialProvider,
            String providerUserId
    );
}