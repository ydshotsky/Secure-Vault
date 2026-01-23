package com.secureVault.security.crypto;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

public class KeyDerivationUtil {
    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;

    public static SecretKey deriveAesKey (
            char[] masterPassword,
            byte[] salt
    )throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(
                masterPassword,
                salt,
                ITERATIONS,
                KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        spec.clearPassword();
        return new SecretKeySpec(keyBytes, "AES");

    }
}
