package com.beautyShop.Opata.Website.entity.repo;

import com.beautyShop.Opata.Website.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<User, UUID> {

    // ── LOGIN / REGISTRATION ─────────────────────────────────
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // ── ADMIN: SEARCH BY EMAIL ───────────────────────────────
    List<User> findByEmailContainingIgnoreCase(String email);

    // ── ADMIN: SEARCH BY NAME ────────────────────────────────
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);
}