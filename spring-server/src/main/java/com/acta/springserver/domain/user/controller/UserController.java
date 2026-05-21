package com.acta.springserver.domain.user.controller;

import com.acta.springserver.common.response.ApiResponse;
import com.acta.springserver.domain.user.dto.MyInfoResponseDto;
import com.acta.springserver.domain.user.service.UserService;
import com.acta.springserver.global.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<MyInfoResponseDto> getMyInfo(Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        MyInfoResponseDto response = userService.getMyInfo(principal.getUserId());

        return ApiResponse.success("내 정보 조회에 성공했습니다.", response);
    }
}