package com.p5store.config;

import com.p5store.domain.User;
import com.p5store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // username is either email (for login) or UUID string (from JWT subject)
        User user = userRepository.findByEmail(username)
                .or(() -> {
                    try {
                        java.util.UUID id = java.util.UUID.fromString(username);
                        return userRepository.findById(id);
                    } catch (IllegalArgumentException e) {
                        return java.util.Optional.empty();
                    }
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())   // UUID is the JWT subject
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(!user.isActive())
                .credentialsExpired(false)
                .disabled(!user.isActive())
                .build();
    }
}