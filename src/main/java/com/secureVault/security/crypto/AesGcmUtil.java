package com.secureVault.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class AesGcmUtil {
    private static final String ALGORITHM ="AES/GCM/NoPadding";
    private static final int TAG_LENGTH=128;

    public static EncryptionResult encrypt(
            byte[] password,
            SecretKey key
    )throws GeneralSecurityException {
        byte[] iv= CryptoUtils.generateIV();

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec=new GCMParameterSpec(TAG_LENGTH,iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] cipherText=cipher.doFinal(password);

        return new EncryptionResult(cipherText,iv);
    }
    public static String decrypt(
            byte[] encryptedPassword,
            byte[] iv,
            SecretKey key
    )throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec=new GCMParameterSpec(TAG_LENGTH,iv);

        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] password=cipher.doFinal(encryptedPassword);
        return new String(password, StandardCharsets.UTF_8);

    }
}
