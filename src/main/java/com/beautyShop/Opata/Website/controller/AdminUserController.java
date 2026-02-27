package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.Config.Security.AdminPrincipal;
import com.beautyShop.Opata.Website.dto.AdminResponse;
import com.beautyShop.Opata.Website.dto.UserResponse;
import com.beautyShop.Opata.Website.dto.UserSummaryResponse;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users", description = "Admin endpoints for viewing and managing customer accounts")
class AdminUserController {

    private final UserService userService;

    private AdminPrincipal adminPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new RuntimeException("User not authenticated");
        Object principal = auth.getPrincipal();
        if (!(principal instanceof AdminPrincipal)) throw new RuntimeException("Invalid authentication principal");
        return (AdminPrincipal) principal;
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Returns total count and full list of registered customers")
    public ResponseEntity<ApiResult<UserSummaryResponse>> getAllUsers() {
        log.info("👥 [ADMIN] Fetching all users");
        return ResponseEntity.ok(ApiResult.success(userService.getAllUsers()));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a single user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResult<UserResponse>> getUserById(@PathVariable UUID userId) {
        log.info("🔍 [ADMIN] Fetching user: {}", userId);
        return ResponseEntity.ok(ApiResult.success(userService.getUserById(userId)));
    }

    @GetMapping("/search/email")
    @Operation(summary = "Search users by email")
    public ResponseEntity<ApiResult<List<UserResponse>>> searchByEmail(@RequestParam String email) {
        log.info("🔍 [ADMIN] Searching users by email: {}", email);
        return ResponseEntity.ok(ApiResult.success(userService.searchUsersByEmail(email)));
    }

    @GetMapping("/search/name")
    @Operation(summary = "Search users by first or last name")
    public ResponseEntity<ApiResult<List<UserResponse>>> searchByName(@RequestParam String name) {
        log.info("🔍 [ADMIN] Searching users by name: {}", name);
        return ResponseEntity.ok(ApiResult.success(userService.searchUsersByName(name)));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Remove a user", description = "Permanently deletes a customer account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User removed"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResult<String>> removeUser(@PathVariable UUID userId) {
        log.info("🗑️ [ADMIN] Removing user: {}", userId);
        String message = userService.removeUser(userId);
        return ResponseEntity.ok(ApiResult.success(message));
    }

    @GetMapping("/admin/profile")
    @Operation(summary = "Get admin profile", description = "Returns the shop owner's profile details")
    public ResponseEntity<ApiResult<AdminResponse>> getAdminProfile() {

        AdminPrincipal adminPrincipal = adminPrincipal();
        UUID adminId = adminPrincipal.getOwnerId();

        log.info("👤 [ADMIN] Fetching admin profile: {}", adminId);
        return ResponseEntity.ok(ApiResult.success(userService.adminProfile(adminId)));
    }
}
