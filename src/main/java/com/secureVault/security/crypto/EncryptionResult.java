package com.secureVault.security.crypto;

public record EncryptionResult (
    byte[] encryptedPassword,
    byte[] iv
    ){}
