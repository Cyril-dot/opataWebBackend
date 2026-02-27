package com.beautyShop.Opata.Website.Config.Security;


import com.beautyShop.Opata.Website.entity.ShopOwner;
import com.beautyShop.Opata.Website.entity.User;
import com.beautyShop.Opata.Website.entity.repo.AdminRepo;
import com.beautyShop.Opata.Website.entity.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;
    private final AdminRepo adminRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        log.debug("🔍 Loading account by email: {}", email);

        // 1️⃣ Check normal users first
        var userOptional = userRepo.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            log.debug("✅ User found: {}, Role: {}", user.getEmail(), user.getRole());
            return new UserPrincipal(user);
        }

        // 2️⃣ If not user, check admin
        var adminOptional = adminRepo.findByEmail(email);
        if (adminOptional.isPresent()) {
            ShopOwner admin = adminOptional.get();
            log.debug("✅ Admin found: {}", admin.getEmail());
            return new AdminPrincipal(admin);
        }

        // 3️⃣ Not found anywhere
        log.warn("❌ No account found with email: {}", email);
        throw new UsernameNotFoundException("No user or admin found with email: " + email);
    }
}