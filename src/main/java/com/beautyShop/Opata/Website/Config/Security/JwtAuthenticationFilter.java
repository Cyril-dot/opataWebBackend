package com.beautyShop.Opata.Website.Config.Security;

import com.beautyShop.Opata.Website.entity.repo.AdminRepo;
import com.beautyShop.Opata.Website.entity.repo.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepo userRepo;
    private final AdminRepo adminRepo;

    private static final List<String> PUBLIC_AUTH_ENDPOINTS = Arrays.asList(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/v1/auth"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (PUBLIC_AUTH_ENDPOINTS.stream().anyMatch(path::startsWith)) return true;
        return path.startsWith("/login")
                || path.startsWith("/api/test/")
                || path.startsWith("/actuator/")
                || path.startsWith("/ws/")
                || path.startsWith("/ws-meeting/")
                || path.equals("/favicon.ico")
                || path.startsWith("/.well-known/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        var existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null
                && existingAuth.isAuthenticated()
                && !(existingAuth instanceof AnonymousAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token  = header.substring(7);
            String email  = tokenService.getEmailFromAccessToken(token);

            var userOptional  = userRepo.findByEmail(email);
            var adminOptional = adminRepo.findByEmail(email);

            if (userOptional.isEmpty() && adminOptional.isEmpty()) {
                log.warn("❌ No user or admin found for email: {}", email);
                filterChain.doFilter(request, response);
                return;
            }

            UserDetails userDetails = userOptional.isPresent()
                    ? new UserPrincipal(userOptional.get())
                    : new AdminPrincipal(adminOptional.get());

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.info("✅ Authentication successful for: {}", email);

        } catch (Exception e) {
            log.error("💥 JWT authentication error: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}