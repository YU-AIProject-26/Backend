package com.acta.springserver.domain.auth.controller;

import com.acta.springserver.common.response.ApiResponse;
import com.acta.springserver.domain.auth.dto.EmailCheckRequestDto;
import com.acta.springserver.domain.auth.dto.EmailCheckResponseDto;
import com.acta.springserver.domain.auth.dto.EmailSendCodeRequestDto;
import com.acta.springserver.domain.auth.dto.EmailVerifyCodeRequestDto;
import com.acta.springserver.domain.auth.dto.LoginRequestDto;
import com.acta.springserver.domain.auth.dto.LoginResponseDto;
import com.acta.springserver.domain.auth.dto.PasswordResetRequestDto;
import com.acta.springserver.domain.auth.dto.PasswordSendCodeRequestDto;
import com.acta.springserver.domain.auth.dto.PasswordVerifyCodeRequestDto;
import com.acta.springserver.domain.auth.dto.SignupRequestDto;
import com.acta.springserver.domain.auth.dto.SignupResponseDto;
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

    @PostMapping("/email/verify-code")
    public ApiResponse<Void> verifyCode(
            @Valid @RequestBody EmailVerifyCodeRequestDto request
    ) {
        authService.verifyCode(request.getEmail(), request.getCode());
        return ApiResponse.success("이메일 인증이 완료되었습니다.", null);
    }

    @PostMapping("/signup")
    public ApiResponse<SignupResponseDto> signup(
            @Valid @RequestBody SignupRequestDto request
    ) {
        SignupResponseDto response = authService.signup(
                request.getNickname(),
                request.getEmail(),
                request.getPassword(),
                request.isTermsAgreed(),
                request.isPrivacyPolicyAgreed()
        );

        return ApiResponse.success("회원가입이 완료되었습니다.", response);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request
    ) {
        LoginResponseDto response = authService.login(
                request.getEmail(),
                request.getPassword()
        );

        return ApiResponse.success("로그인에 성공했습니다.", response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        authService.logout(authorizationHeader);
        return ApiResponse.success("로그아웃이 완료되었습니다.", null);
    }

    @PostMapping("/password/send-code")
    public ApiResponse<Void> sendPasswordResetCode(
            @Valid @RequestBody PasswordSendCodeRequestDto request
    ) {
        authService.sendPasswordResetCode(request.getEmail());
        return ApiResponse.success("비밀번호 재설정 인증 코드가 이메일로 발송되었습니다.", null);
    }

    @PostMapping("/password/verify-code")
    public ApiResponse<Void> verifyPasswordResetCode(
            @Valid @RequestBody PasswordVerifyCodeRequestDto request
    ) {
        authService.verifyPasswordResetCode(request.getEmail(), request.getCode());
        return ApiResponse.success("비밀번호 재설정 인증이 완료되었습니다.", null);
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody PasswordResetRequestDto request
    ) {
        authService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword()
        );
        return ApiResponse.success("비밀번호가 성공적으로 변경되었습니다.", null);
    }
}