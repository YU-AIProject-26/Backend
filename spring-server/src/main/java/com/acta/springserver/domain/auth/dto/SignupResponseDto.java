package com.acta.springserver.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponseDto {

    private Long userId;
    private String email;
    private String nickname;
}