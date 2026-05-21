package com.acta.springserver.domain.auth.service;

import com.acta.springserver.domain.auth.dto.EmailCheckResponseDto;
import com.acta.springserver.domain.auth.dto.LoginResponseDto;
import com.acta.springserver.domain.auth.dto.SignupResponseDto;
import com.acta.springserver.domain.auth.entity.EmailVerification;
import com.acta.springserver.domain.auth.repository.EmailVerificationRepository;
import com.acta.springserver.domain.user.entity.User;
import com.acta.springserver.domain.user.repository.UserRepository;
import com.acta.springserver.global.exception.BusinessException;
import com.acta.springserver.global.exception.ErrorCode;
import com.acta.springserver.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public EmailCheckResponseDto checkEmailDuplicate(String email) {
        boolean exists = userRepository.existsByEmail(email);

        return EmailCheckResponseDto.builder()
                .available(!exists)
                .build();
    }

    @Transactional
    public void sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String code = generateVerificationCode();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(3);

        emailVerificationRepository.deleteByEmail(email);

        EmailVerification emailVerification = EmailVerification.create(email, code, expiredAt);
        emailVerificationRepository.save(emailVerification);

        mailService.sendVerificationCodeEmail(email, code);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        EmailVerification emailVerification = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (emailVerification.isVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        if (emailVerification.isExpired()) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        if (!emailVerification.getCode().equals(code)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }

        emailVerification.markVerified();
    }

    @Transactional
    public SignupResponseDto signup(
            String nickname,
            String email,
            String password,
            boolean termsAgreed,
            boolean privacyPolicyAgreed
    ) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        EmailVerification emailVerification = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (!emailVerification.isVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (emailVerification.isExpired()) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        String encodedPassword = passwordEncoder.encode(password);

        User user = User.create(
                email,
                encodedPassword,
                nickname,
                termsAgreed,
                privacyPolicyAgreed
        );
        user.verifyEmail();

        User savedUser = userRepository.save(user);

        return SignupResponseDto.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .build();
    }

    public LoginResponseDto login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return LoginResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .build();
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}