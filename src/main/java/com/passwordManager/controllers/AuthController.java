package com.passwordManager.controllers;

import com.passwordManager.configuration.CpuBudget;
import com.passwordManager.configuration.UserPrincipal;
import com.passwordManager.security.crypto.CryptoUtils;
import com.passwordManager.user.User;
import com.passwordManager.user.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final PasswordEncoder passwordEncoder;
    private final ExecutorService cpuPool;
    private final UserService userService;
    private final CpuBudget  cpuBudget;


    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@ModelAttribute User user) throws InterruptedException {

        if (!cpuBudget.tryAcquire(200, TimeUnit.MILLISECONDS)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Server busy");
        }

        if (userService.findUserByUsername(user.getUsername()).isPresent())
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("User already exists");

        try {
            Future<?> future = cpuPool.submit(() -> {
                try {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    user.setKdfSalt(CryptoUtils.generateRandomSalt());
                } finally {
                    cpuBudget.release();
                }
            });
            future.get();
            try {
                userService.saveUser(user);
            } catch (Exception e) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("error saving user,please try again later");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing data, please try again later");
        }


        return ResponseEntity.ok("user created successfully");
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<?> deleteProfile(@RequestBody Map<String,
                                                   String> payload,
                                           Authentication authentication,
                                           HttpServletRequest request) throws ServletException {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();

        if (!passwordEncoder.matches(payload.get("password"), user.getPassword()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        userService.deleteUser(user);
        request.logout();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/delete-all")
    public String DeleteProfile() {
        return "deleteProfile";
    }
}
