package com.beautyShop.Opata.Website.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

// ── Returned when viewing user details (hides password) ──────
@Data
@Builder
public class UserResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private LocalDateTime createdAt;
    private int totalOrders; // how many orders this user has placed
}