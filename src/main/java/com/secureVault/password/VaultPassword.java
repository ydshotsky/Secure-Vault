package com.secureVault.password;

import com.secureVault.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vault_passwords")
public class VaultPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Username used on the external site
    private String siteUsername;

    private String email;
    private String phoneNumber;

    // Encrypted password (ciphertext)
    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] encryptedPassword;

    // Initialization Vector used during encryption
    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] iv;

    @Column(columnDefinition = "TEXT")
    private String siteUrl;

    private LocalDate createdAt;

    @Column(length = 2000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
}