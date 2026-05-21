package com.acta.springserver.domain.auth.service;

import com.acta.springserver.domain.auth.dto.EmailCheckResponseDto;
import com.acta.springserver.domain.auth.dto.LoginResponseDto;
import com.acta.springserver.domain.auth.dto.SignupResponseDto;
import com.acta.springserver.domain.auth.entity.BlacklistedToken;
import com.acta.springserver.domain.auth.entity.EmailVerification;
import com.acta.springserver.domain.auth.entity.EmailVerificationPurpose;
import com.acta.springserver.domain.auth.repository.BlacklistedTokenRepository;
import com.acta.springserver.domain.auth.repository.EmailVerificationRepository;
import com.acta.springserver.domain.user.entity.SocialProvider;
import com.acta.springserver.domain.user.entity.User;
import com.acta.springserver.domain.user.repository.UserRepository;
import com.acta.springserver.global.exception.BusinessException;
import com.acta.springserver.global.exception.ErrorCode;
import com.acta.springserver.global.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
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

        emailVerificationRepository.deleteByEmailAndPurpose(email, EmailVerificationPurpose.SIGNUP);

        EmailVerification emailVerification = EmailVerification.create(
                email,
                code,
                EmailVerificationPurpose.SIGNUP,
                expiredAt
        );
        emailVerificationRepository.save(emailVerification);

        mailService.sendVerificationCodeEmail(email, code);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        EmailVerification emailVerification = emailVerificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, EmailVerificationPurpose.SIGNUP)
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

        if (userRepository.existsByNicknameAndDeletedFalse(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        EmailVerification emailVerification = emailVerificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, EmailVerificationPurpose.SIGNUP)
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
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (user.getSocialProvider() != SocialProvider.LOCAL) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_LOCAL_LOGIN_NOT_ALLOWED);
        }

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

    @Transactional
    public void logout(String authorizationHeader) {
        String token = jwtTokenProvider.resolveToken(authorizationHeader);

        if (token == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        try {
            if (blacklistedTokenRepository.existsByToken(token)) {
                return;
            }

            LocalDateTime expiredAt = jwtTokenProvider.getExpiration(token);

            BlacklistedToken blacklistedToken = BlacklistedToken.create(token, expiredAt);
            blacklistedTokenRepository.save(blacklistedToken);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    @Transactional
    public void sendPasswordResetCode(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

        if (user.getSocialProvider() != SocialProvider.LOCAL) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_PASSWORD_RESET_NOT_ALLOWED);
        }

        String code = generateVerificationCode();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(3);

        emailVerificationRepository.deleteByEmailAndPurpose(email, EmailVerificationPurpose.PASSWORD_RESET);

        EmailVerification emailVerification = EmailVerification.create(
                email,
                code,
                EmailVerificationPurpose.PASSWORD_RESET,
                expiredAt
        );
        emailVerificationRepository.save(emailVerification);

        mailService.sendPasswordResetCodeEmail(email, code);
    }

    @Transactional
    public void verifyPasswordResetCode(String email, String code) {
        EmailVerification emailVerification = emailVerificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, EmailVerificationPurpose.PASSWORD_RESET)
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
    public void resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));

        if (user.getSocialProvider() != SocialProvider.LOCAL) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_PASSWORD_RESET_NOT_ALLOWED);
        }

        EmailVerification emailVerification = emailVerificationRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, EmailVerificationPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (!emailVerification.isVerified()) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_NOT_VERIFIED);
        }

        if (emailVerification.isExpired()) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        if (!emailVerification.getCode().equals(code)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(encodedPassword);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}