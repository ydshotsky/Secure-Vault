package com.secureVault.password;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter

public class SavePasswordRequest {
    private String siteUsername;
    private String siteUrl;
    private String email;
    private String phoneNumber;
    private LocalDate createdAt;
    private String notes;
    private String password;
}
