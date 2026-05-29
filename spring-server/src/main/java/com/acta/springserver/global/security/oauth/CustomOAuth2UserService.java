package com.acta.springserver.global.security.oauth;

import com.acta.springserver.domain.user.entity.SocialProvider;
import com.acta.springserver.domain.user.entity.User;
import com.acta.springserver.domain.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);

        SocialProvider socialProvider = SocialProvider.valueOf(registrationId.toUpperCase());

        String email = normalize(userInfo.getEmail());
        String providerUserId = normalize(userInfo.getProviderUserId());
        String nickname = normalize(userInfo.getNickname());

        if (providerUserId == null) {
            throw new OAuth2AuthenticationException("소셜 계정 식별 정보를 가져올 수 없습니다.");
        }

        if (socialProvider != SocialProvider.KAKAO && email == null) {
            throw new OAuth2AuthenticationException("소셜 계정 이메일 정보를 가져올 수 없습니다.");
        }

        User user = userRepository.findBySocialProviderAndProviderUserIdAndDeletedFalse(
                        socialProvider,
                        providerUserId
                )
                .orElseGet(() -> registerOrGetUser(email, nickname, socialProvider, providerUserId));

        return new CustomOAuth2User(user, attributes);
    }

    private User registerOrGetUser(
            String email,
            String nickname,
            SocialProvider socialProvider,
            String providerUserId
    ) {
        if (email != null && userRepository.existsByEmail(email)) {
            throw new OAuth2AuthenticationException("이미 가입된 이메일입니다.");
        }

        String uniqueNickname = createUniqueNickname(nickname, socialProvider);

        User user = User.createSocialUser(
                email,
                uniqueNickname,
                socialProvider,
                providerUserId
        );

        return userRepository.save(user);
    }

    private String createUniqueNickname(String nickname, SocialProvider socialProvider) {
        String baseNickname = (nickname == null || nickname.isBlank())
                ? socialProvider.name().toLowerCase()
                : nickname.trim();

        String candidate = baseNickname;
        while (userRepository.existsByNicknameAndDeletedFalse(candidate)) {
            candidate = baseNickname + "_" + UUID.randomUUID().toString().substring(0, 6);
        }
        return candidate;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}