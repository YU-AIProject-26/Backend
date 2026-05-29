package com.acta.springserver.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponseDto {

    private Long userId;
    private String email;
    private String nickname;
    private String role;
    private String accessToken;
}