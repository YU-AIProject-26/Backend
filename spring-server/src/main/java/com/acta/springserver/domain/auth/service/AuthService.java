package com.acta.springserver.domain.auth.service;

import com.acta.springserver.domain.auth.dto.EmailCheckResponseDto;
import com.acta.springserver.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;

    public EmailCheckResponseDto checkEmailDuplicate(String email) {
        boolean exists = userRepository.existsByEmail(email);

        return EmailCheckResponseDto.builder()
                .available(!exists)
                .build();
    }
}