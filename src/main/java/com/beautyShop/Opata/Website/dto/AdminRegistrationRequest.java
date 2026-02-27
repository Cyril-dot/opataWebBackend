package com.beautyShop.Opata.Website.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// ── Used when the SHOP OWNER / ADMIN registers ───────────────
@Data
public class AdminRegistrationRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Enter a valid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Shop name is required")
    private String shopName;

    @NotBlank(message = "Shop address is required")
    private String shopAddress;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}