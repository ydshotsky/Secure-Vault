package com.secureVault.interceptors;

import com.secureVault.annotations.VaultUnlockedRequired;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;

@Component
public class VaultUnlockInterceptor implements HandlerInterceptor {
    private static final String VAULT_UNLOCKED_AT = "VAULT_UNLOCKED_AT";
    private static final Duration UNLOCK_VALIDITY = Duration.ofMinutes(5);

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod))
            return true;

        VaultUnlockedRequired annotation = handlerMethod.getMethodAnnotation(VaultUnlockedRequired.class);

        if (annotation == null) {
            return true;
        }
        HttpSession session = request.getSession(false);
        Instant unlockedAt = (session != null)
                ? (Instant) session.getAttribute(VAULT_UNLOCKED_AT)
                : null;
        if (unlockedAt == null ||
                Instant.now().isAfter(unlockedAt.plus(UNLOCK_VALIDITY))) {

            // Signal locked state (no redirect)
            response.setStatus(423); // HTTP 423 Locked
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"error\":\"VAULT_LOCKED\"}");

            return false; // Stop request
        }
        return true;


    }
}
