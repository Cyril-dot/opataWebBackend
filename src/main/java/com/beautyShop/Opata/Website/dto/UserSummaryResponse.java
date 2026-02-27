package com.beautyShop.Opata.Website.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// ── Admin dashboard: all users summary ───────────────────────
@Data
@Builder
public class UserSummaryResponse {

    private long totalUsers;             // total count of registered customers
    private List<UserResponse> users;    // list with email + details
}