package com.passwordManager.security.session;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.concurrent.ExecutorService;

@Getter
@RequiredArgsConstructor
public class SessionKeyHolder {
    private SecretKey secretKey;

    public SessionKeyHolder(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public void destroy() {
        if (secretKey != null) {
            byte[]encoded=secretKey.getEncoded();
            if (encoded != null) {
                java.util.Arrays.fill(encoded, (byte)0);
            }
        }
        secretKey = null;
    }
}
