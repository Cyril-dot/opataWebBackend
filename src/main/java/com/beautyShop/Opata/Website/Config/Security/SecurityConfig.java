package com.beautyShop.Opata.Website.Config.Security;

import com.beautyShop.Opata.Website.Config.Security.RateLimitingConfigs.RateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitFilter rateLimitFilter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/error/**", "/actuator/**",
                                "/ws-meeting/**", "/ws/**",
                                "/favicon.ico", "/assets/**",
                                "/.well-known/**", "/ping",
                                "/api/products/**",
                                "/api/auth/**",
                                "/api/v1/products/**"
                        ).permitAll()

                        .requestMatchers("/api/meetings/join/guest").permitAll()
                        .requestMatchers("/api/meetings/daily-token/guest").permitAll()
                        .requestMatchers("/api/meetings/validate/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/meetings/{code}").permitAll()
                        .requestMatchers("/api/meetings/**").authenticated()

                        .requestMatchers("/api/v1/chat/**").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SHOP_OWNER")
                        .requestMatchers("/api/v1/user/**").hasRole("USER")

                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("🚫 Unauthorized: {} {}", request.getMethod(), request.getRequestURI());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"error\": \"Unauthorized\", \"message\": \""
                                            + authException.getMessage() + "\"}");
                        })
                )

                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ✅ Prevents Spring Boot from auto-registering the filter a second time
    // Spring Security already registers it above — this stops the duplicate
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}