package com.beautyShop.Opata.Website.Config.Security;

import com.beautyShop.Opata.Website.entity.ShopOwner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class AdminPrincipal implements UserDetails {

    private ShopOwner owner;


    public UUID getOwnerId() {
        return owner.getId();
    }

    public String getEmail() {
        return owner.getEmail();
    }

    public String getRole() {
        return owner.getRole().name();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + owner.getRole().name()));
    }

    @Override
    public @Nullable String getPassword() {
        return owner.getPassword();
    }

    @Override
    public String getUsername() {
        return owner.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
