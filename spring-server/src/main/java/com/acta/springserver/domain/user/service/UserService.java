package com.acta.springserver.domain.user.service;

import com.acta.springserver.domain.user.dto.MyInfoResponseDto;
import com.acta.springserver.domain.user.dto.NicknameUpdateResponseDto;
import com.acta.springserver.domain.user.entity.User;
import com.acta.springserver.domain.user.repository.UserRepository;
import com.acta.springserver.global.exception.BusinessException;
import com.acta.springserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public MyInfoResponseDto getMyInfo(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return MyInfoResponseDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .role(user.getRole().name())
                .admin(user.getRole().name().equals("ADMIN"))
                .createdAt(user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")))
                .build();
    }

    @Transactional
    public NicknameUpdateResponseDto updateMyNickname(Long userId, String nickname) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.getNickname().equals(nickname) && userRepository.existsByNicknameAndDeletedFalse(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        user.changeNickname(nickname);

        return NicknameUpdateResponseDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .build();
    }

    @Transactional
    public void deleteMyAccount(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.softDelete();
    }
}