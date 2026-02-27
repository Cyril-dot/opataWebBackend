package com.beautyShop.Opata.Website.controller;

import com.beautyShop.Opata.Website.dto.*;
import com.beautyShop.Opata.Website.entity.ApiResult;
import com.beautyShop.Opata.Website.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints for users and admins")
public class AuthController {

    private final RegistrationService registrationService;

    // ═══════════════════════════════════════════════════════════
    // USER REGISTRATION & LOGIN
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/user/register")
    @Operation(
        summary = "Register a new customer",
        description = "Creates a new customer account and returns access + refresh tokens"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or email already in use")
    })
    public ResponseEntity<ApiResult<AuthResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {

        log.info("📝 New user registration: {}", request.getEmail());
        AuthResponse response = registrationService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Registration successful! Welcome " + request.getFirstName(), response));
    }

    @PostMapping("/user/login")
    @Operation(
        summary = "Login as a customer",
        description = "Authenticates a customer and returns access + refresh tokens"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Invalid email or password")
    })
    public ResponseEntity<ApiResult<AuthResponse>> loginUser(
            @Valid @RequestBody LoginRequest request) {

        log.info("🔐 User login attempt: {}", request.getEmail());
        AuthResponse response = registrationService.loginUser(request);
        return ResponseEntity.ok(ApiResult.success("Login successful!", response));
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN REGISTRATION & LOGIN
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/admin/register")
    @Operation(
        summary = "Register a new shop owner / admin",
        description = "Creates a new admin account with shop details and returns access + refresh tokens"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Admin registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error or email already in use")
    })
    public ResponseEntity<ApiResult<AuthResponse>> registerAdmin(
            @Valid @RequestBody AdminRegistrationRequest request) {

        log.info("📝 New admin registration: {} | Shop: {}", request.getEmail(), request.getShopName());
        AuthResponse response = registrationService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success("Admin registered successfully! Welcome " + request.getName(), response));
    }

    @PostMapping("/admin/login")
    @Operation(
        summary = "Login as a shop owner / admin",
        description = "Authenticates an admin and returns access + refresh tokens"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "400", description = "Invalid email or password")
    })
    public ResponseEntity<ApiResult<AuthResponse>> loginAdmin(
            @Valid @RequestBody LoginRequest request) {

        log.info("🔐 Admin login attempt: {}", request.getEmail());
        AuthResponse response = registrationService.loginAdmin(request);
        return ResponseEntity.ok(ApiResult.success("Login successful!", response));
    }
}