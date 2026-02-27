package com.beautyShop.Opata.Website.dto;

import lombok.Data;

// ── Used when a user updates their own profile ───────────────
@Data
public class UpdateUserRequest {

    private String firstName;
    private String lastName;
    private String phone;

    // User cannot change their own email or role via this DTO
    // If password change is needed, create a separate ChangePasswordRequest
}