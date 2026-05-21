package com.acta.springserver.global.security.oauth;

public interface OAuth2UserInfo {

    String getProviderUserId();

    String getEmail();

    String getNickname();
}