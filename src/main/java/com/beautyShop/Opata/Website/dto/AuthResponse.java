package com.beautyShop.Opata.Website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;   // short-lived encrypted JWT
    private String refreshToken;  // long-lived encrypted refresh token
    private String role;          // "USER" or "ADMIN"
    private String email;
    private String name;          // firstName+lastName for user, shopName for admin
    private String message;       // e.g. "Login successful!"
}