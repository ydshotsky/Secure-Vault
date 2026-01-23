package com.secureVault.user;

import com.secureVault.password.VaultPassword;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class User {
    @Id
    @SequenceGenerator(name = "user_seq", sequenceName = "user_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    Long id;

    @Column(unique = true, nullable = false)
    private String username;

    //hashed master key
    @Column(nullable = false)
    private String password;

    // salt for PBKDF2 / Argon2
    @Column(nullable = false, columnDefinition = "BYTEA", unique = true)
    private byte[] kdfSalt;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<VaultPassword> passwords;
}
