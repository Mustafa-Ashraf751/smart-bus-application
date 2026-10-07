package com.verysmartbus.security;

import com.verysmartbus.entity.AppUser;
import com.verysmartbus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return UserPrincipal.from(user);
    }

    @Transactional(readOnly = true)
    public UserPrincipal loadUserById(Long userId) {
        AppUser user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        return UserPrincipal.from(user);
    }
}
