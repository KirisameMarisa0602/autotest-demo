package com.example.derivatives.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Demo identity is supplied via headers rather than real authentication.
 * Required: X-Demo-User, X-Demo-Role.
 * Optional tracing: X-Run-Id, X-Case-Key (accepted and available to audit callers).
 */
@Component
public class DemoIdentityFilter extends OncePerRequestFilter {
    public static final String HEADER_USER = "X-Demo-User";
    public static final String HEADER_ROLE = "X-Demo-Role";
    public static final String HEADER_RUN_ID = "X-Run-Id";
    public static final String HEADER_CASE_KEY = "X-Case-Key";

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "trader", "risk_manager", "viewer");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String user = blankToNull(request.getHeader(HEADER_USER));
            String role = normalizeRole(request.getHeader(HEADER_ROLE));
            if (user != null && role != null && ALLOWED_ROLES.contains(role)) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        role,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authentication.setDetails(new DemoRequestContext(
                        user,
                        role,
                        blankToNull(request.getHeader(HEADER_RUN_ID)),
                        blankToNull(request.getHeader(HEADER_CASE_KEY))
                ));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private static String normalizeRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record DemoRequestContext(String user, String role, String runId, String caseKey) {
    }
}
