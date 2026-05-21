package com.acta.springserver.domain.auth.controller;

import com.acta.springserver.common.response.ApiResponse;
import com.acta.springserver.domain.auth.dto.EmailCheckRequestDto;
import com.acta.springserver.domain.auth.dto.EmailCheckResponseDto;
import com.acta.springserver.domain.auth.dto.EmailSendCodeRequestDto;
import com.acta.springserver.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/check-email")
    public ApiResponse<EmailCheckResponseDto> checkEmail(
            @Valid @RequestBody EmailCheckRequestDto request
    ) {
        EmailCheckResponseDto response = authService.checkEmailDuplicate(request.getEmail());

        String message = response.isAvailable()
                ? "사용 가능한 이메일입니다."
                : "이미 사용 중인 이메일입니다.";

        return ApiResponse.success(message, response);
    }

    @PostMapping("/email/send-code")
    public ApiResponse<Void> sendVerificationCode(
            @Valid @RequestBody EmailSendCodeRequestDto request
    ) {
        authService.sendVerificationCode(request.getEmail());
        return ApiResponse.success("인증 코드가 이메일로 발송되었습니다.", null);
    }
}