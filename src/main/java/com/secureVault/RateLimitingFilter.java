package com.secureVault;

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

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {
    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws IOException, ServletException {
        String uri = request.getRequestURI();
        int maxRequestsPerMinute = 100; // Increased default for general assets
        String bucket = "general";

        if (uri.startsWith("/vault/")) {
            bucket = "vault";
            maxRequestsPerMinute = 10;  // Tight limit for heavy cryptographic operations (PBKDF2/Argon2)
        } else if (uri.startsWith("/auth/")) {
            bucket = "auth";
            maxRequestsPerMinute = 10; // Increased slightly to accommodate login + initial redirects
        } else if (uri.startsWith("/password/")) {
            bucket = "password";
            maxRequestsPerMinute = 40;
        } else if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/")) {
            bucket = "static";
            maxRequestsPerMinute = 200; // Very high limit for static assets
        }

        // Logic to prevent bot overwhelm and ensure server stability
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp==null||clientIp.isEmpty())
            clientIp = request.getRemoteAddr();

        if(clientIp!=null&&clientIp.contains(","))
            clientIp = clientIp.split(",")[0].trim();
        boolean isAllowed=rateLimiterService.isAllowed(clientIp, bucket, maxRequestsPerMinute);

        if (!isAllowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            String acceptHeader = request.getHeader("Accept");
            if (acceptHeader != null && acceptHeader.contains("text/html")) {
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.getWriter().write("<div style='color:red;'>Too Many Requests. Rate limit exceeded. Please try again in a minute.</div>");
            } else {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again in a minute.\"}");
            }
            return;
        }
        filterChain.doFilter(request, response);
    }

}
