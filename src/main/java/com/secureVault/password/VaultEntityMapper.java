package com.secureVault.password;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VaultEntityMapper {

    public VaultPassword getVaultEntity(VaultDto vaultDto) {
        return VaultPassword
                .builder()
                .email(vaultDto.getEmail())
                .notes(vaultDto.getNotes())
                .createdAt(vaultDto.getCreatedAt())
                .phoneNumber(vaultDto.getPhoneNumber())
                .siteUsername(vaultDto.getSiteUsername())
                .siteUrl(vaultDto.getSiteUrl())
                .build();
    }

    public VaultDto getVaultDto(VaultPassword password) {
        return VaultDto
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
