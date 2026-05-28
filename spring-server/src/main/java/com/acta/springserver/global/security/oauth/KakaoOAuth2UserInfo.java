package com.acta.springserver.global.security.oauth;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> profile;

    @SuppressWarnings("unchecked")
    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.profile = kakaoAccount != null
                ? (Map<String, Object>) kakaoAccount.get("profile")
                : null;
    }

    @Override
    public String getProviderUserId() {
        Object id = attributes.get("id");
        return id != null ? id.toString() : null;
    }

    @Override
    public String getEmail() {
        if (kakaoAccount == null) {
            return null;
        }
        Object email = kakaoAccount.get("email");
        return email != null ? email.toString() : null;
    }

    @Override
    public String getNickname() {
        if (profile == null) {
            return null;
        }
        Object nickname = profile.get("nickname");
        return nickname != null ? nickname.toString() : null;
    }
}