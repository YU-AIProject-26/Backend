package com.acta.springserver.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyInfoResponseDto {

    private Long userId;
    private String nickname;
    private String email;
    private String role;
    private boolean admin;
    private String createdAt;
}