package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.Config.Security.UserPrincipal;
import com.beautyShop.Opata.Website.dto.*;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


// ═══════════════════════════════════════════════════════════════
// USER PROFILE CONTROLLER
// ═══════════════════════════════════════════════════════════════

@Slf4j
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Tag(name = "User Profile", description = "Profile endpoints for logged-in customers")
class UserProfileController {

    private final UserService userService;


    private UserPrincipal userPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.error("No authentication found in SecurityContext");
            throw new RuntimeException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal)) {
            log.error("Invalid principal type: {}", principal != null ? principal.getClass().getName() : "null");
            throw new RuntimeException("Invalid authentication principal");
        }

        UserPrincipal userPrincipal = (UserPrincipal) principal;
        log.debug("Successfully retrieved UserPrincipal for user: {} (ID: {})",
                userPrincipal.getUsername(), userPrincipal.getUserId());

        return userPrincipal;
    }



    @GetMapping("/")
    @Operation(summary = "Get my profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile retrieved"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResult<UserResponse>> getMyProfile() {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("👤 Fetching profile for user: {}", userId);
        return ResponseEntity.ok(ApiResult.success(userService.getMyProfile(userId)));
    }

    @PutMapping("/")
    @Operation(summary = "Update my profile", description = "Update first name, last name, and/or phone number")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResult<UserResponse>> updateMyProfile(
            @Valid @RequestBody UpdateUserRequest request) {
        UserPrincipal principal = userPrincipal();
        UUID userId = principal.getUserId();
        log.info("✏️ Updating profile for user: {}", userId);
        UserResponse response = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(ApiResult.success("Profile updated successfully", response));
    }
}


