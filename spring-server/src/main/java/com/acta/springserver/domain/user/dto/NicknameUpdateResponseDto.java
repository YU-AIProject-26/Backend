package com.acta.springserver.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NicknameUpdateResponseDto {

    private Long userId;
    private String nickname;
}