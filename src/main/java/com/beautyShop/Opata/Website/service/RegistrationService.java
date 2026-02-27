package com.beautyShop.Opata.Website.service;


import com.beautyShop.Opata.Website.Config.Security.TokenService;
import com.beautyShop.Opata.Website.Config.Security.entity.AdminRefreshToken;
import com.beautyShop.Opata.Website.Config.Security.entity.RefreshToken;
import com.beautyShop.Opata.Website.dto.*;
import com.beautyShop.Opata.Website.entity.*;
import com.beautyShop.Opata.Website.entity.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepo userRepository;
    private final AdminRepo       shopOwnerRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // ═══════════════════════════════════════════════════════════
    // REGISTER CUSTOMER
    // ═══════════════════════════════════════════════════════════
    public AuthResponse registerUser(UserRegistrationRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        // Generate encrypted access token + encrypted refresh token
        String accessToken  = tokenService.generateAccessToken(user);
        RefreshToken refresh = tokenService.generateRefreshToken(user);

        log.info("✅ New customer registered: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refresh.getToken())
                .role(Role.USER.name())
                .email(user.getEmail())
                .name(user.getFirstName() + " " + user.getLastName())
                .message("Registration successful! Welcome " + user.getFirstName())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // REGISTER SHOP OWNER / ADMIN
    // ═══════════════════════════════════════════════════════════
    public AuthResponse registerAdmin(AdminRegistrationRequest request) {

        if (shopOwnerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        ShopOwner admin = ShopOwner.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .shopName(request.getShopName())
                .shopAddress(request.getShopAddress())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .build();

        shopOwnerRepository.save(admin);

        // Generate encrypted access token + encrypted refresh token for admin
        String accessToken          = tokenService.generateOwnerAccessToken(admin);
        AdminRefreshToken refresh   = tokenService.generateAdminRefreshToken(admin);

        log.info("✅ New admin registered: {} | Shop: {}", admin.getEmail(), admin.getShopName());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refresh.getToken())
                .role(Role.ADMIN.name())
                .email(admin.getEmail())
                .name(admin.getShopName())
                .message("Admin registered successfully! Welcome " + admin.getName())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // LOGIN CUSTOMER
    // ═══════════════════════════════════════════════════════════
    public AuthResponse loginUser(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with email: " + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }

        // Generate fresh encrypted access + refresh tokens on every login
        String accessToken   = tokenService.generateAccessToken(user);
        RefreshToken refresh  = tokenService.generateRefreshToken(user);

        log.info("🔐 Customer logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refresh.getToken())
                .role(Role.USER.name())
                .email(user.getEmail())
                .name(user.getFirstName() + " " + user.getLastName())
                .message("Login successful! Welcome back " + user.getFirstName())
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // LOGIN SHOP OWNER / ADMIN
    // ═══════════════════════════════════════════════════════════
    public AuthResponse loginAdmin(LoginRequest request) {

        ShopOwner admin = shopOwnerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No admin account found with email: " + request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }

        // Generate fresh encrypted access + refresh tokens on every login
        String accessToken        = tokenService.generateOwnerAccessToken(admin);
        AdminRefreshToken refresh  = tokenService.generateAdminRefreshToken(admin);

        log.info("🔐 Admin logged in: {} | Shop: {}", admin.getEmail(), admin.getShopName());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refresh.getToken())
                .role(Role.ADMIN.name())
                .email(admin.getEmail())
                .name(admin.getShopName())
                .message("Login successful! Welcome back " + admin.getName())
                .build();
    }
}