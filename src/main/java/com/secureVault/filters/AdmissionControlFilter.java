package com.secureVault.filters;

import com.secureVault.configuration.CpuBudget;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
@Component
@RequiredArgsConstructor
public class AdmissionControlFilter extends OncePerRequestFilter {

    private final CpuBudget cpuBudget;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        long timeout = 50;

        if(uri.equals("/auth/login")&&method.equals("POST")) {
            timeout=500;
        }
        if(uri.startsWith("password/reveal/")&&method.equals("POST")) {
            timeout=100;
        }
        if(uri.equals("/vault/unlock")&&method.equals("POST")) {
            timeout=200;
        }
        // Identify heavy cryptographic operations (PBKDF2/Argon2/BCrypt/AES-GCM)
        boolean isHeavy = (uri.equals("/auth/login") && method.equals("POST")) ||
                          (uri.startsWith("/vault/unlock") && method.equals("POST")) ||
                          (uri.startsWith("/password/reveal") && method.equals("POST"));
        if (isHeavy) {
            boolean acquired = false;
            try {
                // Try to acquire a CPU permit. If full, reject the request (Load Shedding)
                // We use a small timeout to avoid long-blocking the servlet thread
                acquired = cpuBudget.tryAcquire(timeout, TimeUnit.MILLISECONDS);
                
                if (!acquired) {
                    response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                    response.setContentType(MediaType.TEXT_HTML_VALUE);
                    response.getWriter().write("<h2>Server Overloaded</h2><p>The system is currently handling maximum cryptographic load. Please try again in a few seconds.</p>");
                    return;
                }
                
                // Proceed with the request if permit is acquired
                filterChain.doFilter(request, response);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Error during admission control");
            } finally {
                if (acquired) {
                    // Release the CPU permit for the next waiting request
                    cpuBudget.release();
                }
            }
        } else {
            // Lightweight requests skip the CPU budget check
            filterChain.doFilter(request, response);
        }
    }
}
