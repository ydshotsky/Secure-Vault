package com.passwordManager.password;

import com.passwordManager.user.User;
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
    @SequenceGenerator(
            name = "vault_password_seq",
            sequenceName = "vault_password_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "vault_password_seq"
    )
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

    private String siteUrl;

    private LocalDate createdAt;

    @Column(length = 2000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
}