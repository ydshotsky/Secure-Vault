package com.passwordManager.password;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordEntityMapper {

    public VaultPassword getPasswordEntity(PasswordDto passwordDto) {
        return VaultPassword
                .builder()
                .email(passwordDto.getEmail())
                .notes(passwordDto.getNotes())
                .createdAt(passwordDto.getCreatedAt())
                .phoneNumber(passwordDto.getPhoneNumber())
                .siteUsername(passwordDto.getSiteUsername())
                .siteUrl(passwordDto.getSiteUrl())
                .build();
    }

    public PasswordDto getPasswordDto(VaultPassword password) {
        return PasswordDto
                .builder()
                .id(password.getId())
                .email(password.getEmail())
                .notes(password.getNotes())
                .createdAt(password.getCreatedAt())
                .phoneNumber(password.getPhoneNumber())
                .siteUsername(password.getSiteUsername())
                .siteUrl(password.getSiteUrl())
                .build();
    }
}
