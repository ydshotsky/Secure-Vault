package com.passwordManager.password;

import com.passwordManager.annotations.VaultUnlockedRequired;
import com.passwordManager.configuration.CpuBudget;
import com.passwordManager.configuration.UserPrincipal;
import com.passwordManager.security.crypto.AesGcmUtil;
import com.passwordManager.security.crypto.EncryptionResult;
import com.passwordManager.security.session.SessionKeyHolder;
import com.passwordManager.user.User;
import com.passwordManager.user.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/password")
public class PasswordController {
    private final PasswordEntityMapper passwordEntityMapper;
    private final PasswordService passwordService;
    private final ExecutorService cpuPool;
    private final UserService userService;
    private final CpuBudget cpuBudget;

    public SessionKeyHolder getActiveVaultKey(HttpSession session) {
        SessionKeyHolder sessionKeyHolder = (SessionKeyHolder) session.getAttribute("VAULT_KEY");
        return sessionKeyHolder != null && sessionKeyHolder.getSecretKey() != null ? sessionKeyHolder : null;
    }

    @VaultUnlockedRequired
    @PostMapping("/reveal/{id}")
    public ResponseEntity<String> revealPassword(
            @PathVariable Long id,
            HttpSession session,
            Principal principal
    ) throws InterruptedException {
        if (!cpuBudget.tryAcquire(200, TimeUnit.MILLISECONDS)) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("server is busy, please try again later");
        }
        SessionKeyHolder sessionKeyHolder = getActiveVaultKey(session);
        if (sessionKeyHolder == null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("vault is locked, please enter master key");
        }

        VaultPassword password = passwordService.findPasswordById(id).orElse(null);

        if (password == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!password.getUser().getUsername().equals(principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {

            Future<String> decryptedPasswordFuture = cpuPool.submit(() -> {
                        try {
                            return AesGcmUtil.decrypt(
                                    password.getEncryptedPassword(),
                                    password.getIv(),
                                    sessionKeyHolder.getSecretKey()
                            );
                        } finally {
                            cpuBudget.release();
                        }
                    }
            );
            String decryptedPassword = decryptedPasswordFuture.get();
            return ResponseEntity.ok(decryptedPassword);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

    @GetMapping("/create-password-entry")
    public String passwordEntryForm(Model model) {
        model.addAttribute("passwordRequest", new SavePasswordRequest());
        return "password-entry-form";
    }


    @PostMapping("/save-vault-password")
    @Transactional
    @VaultUnlockedRequired
    public ResponseEntity<String> savePassword(
            @ModelAttribute SavePasswordRequest savePasswordRequest,
            HttpSession session,
            Authentication authentication) throws ExecutionException, InterruptedException {
        SessionKeyHolder sessionKeyHolder = getActiveVaultKey(session);
        if (sessionKeyHolder == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("vault is locked, please enter master key");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        VaultPassword vaultPassword = VaultPassword
                .builder()
                .siteUrl(savePasswordRequest.getSiteUrl())
                .siteUsername(savePasswordRequest.getSiteUsername())
                .phoneNumber(savePasswordRequest.getPhoneNumber())
                .createdAt(savePasswordRequest.getCreatedAt())
                .email(savePasswordRequest.getEmail())
                .notes(savePasswordRequest.getNotes())
                .build();

        User user = userService
                .findUserByUsername(userPrincipal.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));

        vaultPassword.setUser(user);

        SecretKey key = sessionKeyHolder.getSecretKey();
        Future<EncryptionResult> encryptionResultFuture = cpuPool
                .submit(() -> AesGcmUtil
                        .encrypt(savePasswordRequest
                                        .getPassword()
                                        .getBytes(),
                                key)
                );

        EncryptionResult encryptionResult = encryptionResultFuture.get();

        vaultPassword.setEncryptedPassword(encryptionResult.encryptedPassword());

        vaultPassword.setIv(encryptionResult.iv());

        try {
            passwordService.savePassword(vaultPassword);
        } catch (Exception e) {
            System.out.println("error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
        return ResponseEntity.ok("password saved successfully");
    }

    @GetMapping("/password-list")
    public String passwordListForm(Model model, Authentication authentication) {
        List<VaultPassword> passwords = passwordService
                .findPasswordByUserUsername(authentication.getName());
        List<PasswordDto> passwordDtos = passwords
                .stream()
                .map(passwordEntityMapper::getPasswordDto)
                .toList();


        if (passwordDtos.isEmpty())
            model.addAttribute("message", "No Passwords Saved Yet");
        else
            model.addAttribute("passwords", passwordDtos);
        return "password-list";
    }

    @GetMapping("/search-password")
    public String searchPassword(@RequestParam(value = "keyword", required = false) String keyword, Model model, Authentication authentication) throws ExecutionException, InterruptedException {
        if (keyword == null || keyword.trim().isEmpty()) {
            model.addAttribute("message", "Please enter a keyword to search.");
            return "search-password";
        }
        Future<List<VaultPassword>> future = cpuPool.submit(() ->
                passwordService
                        .findPasswordByKeywordAndUserUsername(
                                keyword,
                                authentication.getName())
        );

        List<VaultPassword> passwords = future.get();
        List<PasswordDto> passwordDtos = passwords
                .stream()
                .map(passwordEntityMapper::getPasswordDto)
                .toList();
        if (passwordDtos.isEmpty()) {
            model.addAttribute("message", "No passwords found for '" + keyword + "'");
        } else {
            model.addAttribute("passwords", passwordDtos);
        }
        return "search-password";
    }


    @DeleteMapping("/delete/{id}")
    @VaultUnlockedRequired
    public ResponseEntity<String> deletePassword(@PathVariable("id") Long id, HttpSession session) {
        SessionKeyHolder sessionKeyHolder = getActiveVaultKey(session);
        if (sessionKeyHolder == null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("vault is locked, please enter login password");
        }
        passwordService.deletePasswordById(id);
        return ResponseEntity.ok("password deleted successfully");
    }
}
