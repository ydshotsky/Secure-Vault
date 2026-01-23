package com.passwordManager.controllers;

import com.passwordManager.configuration.CpuBudget;
import com.passwordManager.security.crypto.KeyDerivationUtil;
import com.passwordManager.security.session.SessionKeyHolder;
import com.passwordManager.user.User;
import com.passwordManager.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.crypto.SecretKey;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
@RequestMapping("/vault")
public class VaultController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExecutorService cpuPool;
    private final CpuBudget cpuBudget;

    @PostMapping("/unlock")
    public ResponseEntity<Boolean> unlockVault(
            @RequestBody Map<String, String> payload,
            HttpSession session,
            Authentication authentication
    ) throws InterruptedException {

        if(!authentication.isAuthenticated())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if(!cpuBudget.tryAcquire(200, TimeUnit.MILLISECONDS))
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow();
        SecretKey key;
        try {
            Future<SecretKey> secretKeyFuture = cpuPool.submit(() -> {
                try {


                if (!passwordEncoder.matches(payload.get("password"), user.getPassword()))
                    return null;
                return KeyDerivationUtil.deriveAesKey(
                        payload.get("password").toCharArray(),
                        user.getKdfSalt());
                }
                finally {
                    cpuBudget.release();
                }
            });
            key=secretKeyFuture.get();
        }
        catch (InterruptedException | ExecutionException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (key == null) {
            return ResponseEntity.ok(false);
        }

        session.setAttribute("VAULT_KEY", new SessionKeyHolder(key));
        session.setAttribute("VAULT_UNLOCKED_AT", Instant.now());
        return ResponseEntity.ok(true);
    }


    @PostMapping("/lock")
    public void lockVault(HttpSession session) {
        SessionKeyHolder sessionKeyHolder = (SessionKeyHolder) session.getAttribute("VAULT_KEY");
        if (sessionKeyHolder != null) {
            sessionKeyHolder.destroy();
        }
        session.removeAttribute("VAULT_KEY");
        session.removeAttribute("VAULT_UNLOCKED_AT");
    }
}
