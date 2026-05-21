package com.acta.springserver.global.security.oauth;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> response;

    @SuppressWarnings("unchecked")
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.response = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getProviderUserId() {
        if (response == null) {
            return null;
        }
        Object id = response.get("id");
        return id != null ? id.toString() : null;
    }

    @Override
    public String getEmail() {
        if (response == null) {
            return null;
        }
        Object email = response.get("email");
        return email != null ? email.toString() : null;
    }

    @Override
    public String getNickname() {
        if (response == null) {
            return null;
        }
        Object nickname = response.get("nickname");
        if (nickname != null) {
            return nickname.toString();
        }

        Object name = response.get("name");
        return name != null ? name.toString() : null;
    }
}