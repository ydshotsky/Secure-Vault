package com.secureVault.security.crypto;

import java.security.SecureRandom;

public class CryptoUtils {
    private static final int IV_LENGTH=12;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom secureRandom = new SecureRandom();
    public static byte[] generateRandomSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }
    public static byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }
}
