package com.passwordManager.password;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordDto {
    private Long id;
    private String siteUsername;
    private String siteUrl;
    private String email;
    private String phoneNumber;
    private LocalDate createdAt;
    private String notes;
}
