package com.acta.springserver.domain.user.controller;

import com.acta.springserver.common.response.ApiResponse;
import com.acta.springserver.domain.auth.service.AuthService;
import com.acta.springserver.domain.user.dto.MyInfoResponseDto;
import com.acta.springserver.domain.user.dto.NicknameUpdateRequestDto;
import com.acta.springserver.domain.user.dto.NicknameUpdateResponseDto;
import com.acta.springserver.domain.user.service.UserService;
import com.acta.springserver.global.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/me")
    public ApiResponse<MyInfoResponseDto> getMyInfo(Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        MyInfoResponseDto response = userService.getMyInfo(principal.getUserId());

        return ApiResponse.success("내 정보 조회에 성공했습니다.", response);
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<NicknameUpdateResponseDto> updateMyNickname(
            Authentication authentication,
            @Valid @RequestBody NicknameUpdateRequestDto request
    ) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        NicknameUpdateResponseDto response = userService.updateMyNickname(
                principal.getUserId(),
                request.getNickname()
        );

        return ApiResponse.success("닉네임이 성공적으로 변경되었습니다.", response);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMyAccount(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();

        userService.deleteMyAccount(principal.getUserId());
        authService.logout(authorizationHeader);

        return ApiResponse.success("회원 탈퇴가 완료되었습니다.", null);
    }
}