package com.secureVault.controllers;


import com.secureVault.password.VaultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final VaultRepository vaultRepository;

    @GetMapping("/")
    public String Dashboard(Model model, Principal principal) {
        String username = principal.getName();
        long passwordCount = vaultRepository.countByUserUsername(username);
        model.addAttribute("username", username);
        model.addAttribute("passwordCount", passwordCount);
        return "Dashboard";
    }
}
