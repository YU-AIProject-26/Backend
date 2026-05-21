package com.acta.springserver.domain.auth.service;

import com.acta.springserver.domain.auth.dto.EmailCheckResponseDto;
import com.acta.springserver.domain.auth.entity.EmailVerification;
import com.acta.springserver.domain.auth.repository.EmailVerificationRepository;
import com.acta.springserver.domain.user.repository.UserRepository;
import com.acta.springserver.global.exception.BusinessException;
import com.acta.springserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
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

    private String generateVerificationCode() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }
}