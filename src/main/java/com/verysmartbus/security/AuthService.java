package com.verysmartbus.security;

import com.verysmartbus.dto.request.LoginRequestDto;
import com.verysmartbus.dto.request.RegisterRequestDto;
import com.verysmartbus.dto.response.AuthResponseDto;
import com.verysmartbus.dto.response.AuthUserDto;
import com.verysmartbus.dto.response.RegistrationResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Role;
import com.verysmartbus.entity.enums.UserStatus;
import com.verysmartbus.repository.RoleRepository;
import com.verysmartbus.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String PASSENGER_ROLE = "EMPLOYEE";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponseDto login(LoginRequestDto dto) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(dto.identifier(), dto.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(principal);

        return new AuthResponseDto(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                AuthUserDto.from(principal)
        );
    }

    /**
     * Public registration can create only a normal passenger account. Operational roles remain
     * an admin responsibility, so a caller can never self-register as ADMIN, DRIVER or BUS_ADMIN.
     */
    @Transactional
    public RegistrationResponseDto register(RegisterRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalStateException("Email already registered.");
        }
        if (userRepository.existsByUsernameIgnoreCase(dto.username())) {
            throw new IllegalStateException("Username already registered.");
        }

        Role passengerRole = roleRepository.findByName(PASSENGER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default EMPLOYEE role is not configured."));

        AppUser user = AppUser.builder()
                .name(dto.name())
                .username(dto.username())
                .phone(dto.phone())
                .address(dto.address())
                .email(dto.email())
                .passwordHash(passwordEncoder.encode(dto.password()))
                .status(UserStatus.PENDING)
                .build();
        user.getRoles().add(passengerRole);

        AppUser saved = userRepository.save(user);
        return new RegistrationResponseDto(
                saved.getId(), saved.getUsername(), saved.getEmail(), saved.getStatus(),
                "Registration received. An administrator must approve your account before you can log in."
        );
    }
}
