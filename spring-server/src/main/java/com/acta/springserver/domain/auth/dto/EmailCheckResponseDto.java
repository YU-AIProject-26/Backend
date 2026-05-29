package com.acta.springserver.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailCheckResponseDto {
    private final boolean available;
}