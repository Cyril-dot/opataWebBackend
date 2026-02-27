package com.beautyShop.Opata.Website.service;

import com.beautyShop.Opata.Website.dto.*;
import com.beautyShop.Opata.Website.entity.*;
import com.beautyShop.Opata.Website.entity.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepository;
    private final AdminRepo ownerRepo;
    private final OrderRepository orderRepository;

    // ── GET MY PROFILE (logged-in user) ──────────────────────
    public UserResponse getMyProfile(UUID userId) {
        User user = findUserById(userId);
        return mapToResponse(user);
    }

    // ── UPDATE MY PROFILE ────────────────────────────────────
    public UserResponse updateMyProfile(UUID userId, UpdateUserRequest request) {
        User user = findUserById(userId);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getPhone()     != null) user.setPhone(request.getPhone());

        userRepository.save(user);
        System.out.println("✅ Profile updated for user: " + user.getEmail());
        return mapToResponse(user);
    }

    // ── ADMIN: GET ALL USERS SUMMARY ─────────────────────────
    // Returns total count + list of users with their emails
    public UserSummaryResponse getAllUsers() {
        List<User> users = userRepository.findAll();
        long total = users.size();

        System.out.println("═══════════════════════════════════");
        System.out.println("👥 Total Registered Customers: " + total);
        System.out.println("═══════════════════════════════════");
        users.forEach(u -> System.out.println(
            "  [" + u.getId() + "] " + u.getFirstName() + " " + u.getLastName() +
            " | " + u.getEmail() +
            " | Orders: " + orderRepository.countByUserId(u.getId())
        ));

        List<UserResponse> userResponses = users.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        return UserSummaryResponse.builder()
            .totalUsers(total)
            .users(userResponses)
            .build();
    }

    // ── ADMIN: GET SINGLE USER DETAILS ───────────────────────
    public UserResponse getUserById(UUID userId) {
        User user = findUserById(userId);
        return mapToResponse(user);
    }

    // to get admin profile and detials
    public AdminResponse adminProfile(UUID adminId){
        ShopOwner owner = ownerRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found with id: " + adminId));
        return AdminResponse.builder()
                .id(owner.getId())
                .name(owner.getName())
                .email(owner.getEmail())
                .phone(owner.getPhone())
                .shopName(owner.getShopName())
                .role(owner.getRole())
                .build();
    }


    // ── ADMIN: SEARCH USERS BY EMAIL ─────────────────────────
    public List<UserResponse> searchUsersByEmail(String email) {
        return userRepository.findByEmailContainingIgnoreCase(email)
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── ADMIN: SEARCH USERS BY NAME ──────────────────────────
    public List<UserResponse> searchUsersByName(String name) {
        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(name, name)
            .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── ADMIN: REMOVE A USER ─────────────────────────────────
    public String removeUser(UUID userId) {
        User user = findUserById(userId);
        userRepository.delete(user);
        System.out.println("🗑️  User removed: " + user.getEmail());
        return "User " + user.getFirstName() + " " + user.getLastName() +
               " (" + user.getEmail() + ") has been removed successfully.";
    }

    // ── PRIVATE HELPERS ──────────────────────────────────────

    private User findUserById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .role(user.getRole().name())
            .createdAt(user.getCreatedAt())
            .totalOrders((int) orderRepository.countByUserId(user.getId()))
            .build();
    }
}