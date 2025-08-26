package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("Use phone+merchant for auth");
    }

    public UserDetails loadUserById(Integer userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getPhone())
                .password(u.getPasswordHash() == null ? "{noop}" : u.getPasswordHash())
                .accountLocked(!u.getActive())
                .build();
    }
}
