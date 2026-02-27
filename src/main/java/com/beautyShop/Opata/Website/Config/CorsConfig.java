package com.beautyShop.Opata.Website.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",
                "https://*.ngrok-free.dev",
                "https://cyril-dot.github.io",
                "http://127.0.0.1:5500",
                "http://localhost:5500",
                "http://localhost:8081",
                "http://192.168.8.127:8081",
                "https://novaspace-teal.vercel.app",
                "https://novaspace-3xjlmad36-cyril-dots-projects.vercel.app",
                "https://billion-laptops.vercel.app",
                "https://billion-laptops-admin.vercel.app",
                "https://billion-laptops-admin-366n8r9uk-cyril-dots-projects.vercel.app",
                "https://billion-laptops-admin-git-master-cyril-dots-projects.vercel.app"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // false because we use token-based auth (Bearer), not cookies
        config.setAllowCredentials(false);

        config.setMaxAge(3600L);

        config.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}