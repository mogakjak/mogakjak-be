package com.mogakjak.mogakjak.global.auth.security;

import com.mogakjak.mogakjak.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor(staticName = "of")
public class CustomUserDetails implements UserDetails {

    private final User user;

    public boolean isDeleted() {
        return Boolean.TRUE.equals(user.getIsDeleted());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return user.getId().toString();
    }

    @Override
    public boolean isAccountNonLocked() { return !isDeleted(); }

    @Override
    public boolean isEnabled() { return !isDeleted(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
}
