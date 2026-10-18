package com.verysmartbus.service;

import com.verysmartbus.dto.request.UserRequestDto;
import com.verysmartbus.dto.response.UserResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Role;
import com.verysmartbus.entity.enums.UserStatus;
import com.verysmartbus.mapper.UserMapper;
import com.verysmartbus.repository.RoleRepository;
import com.verysmartbus.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDto create(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalStateException("Email already registered: " + dto.email());
        }
        if (userRepository.existsByUsernameIgnoreCase(dto.username())) {
            throw new IllegalStateException("Username already registered: " + dto.username());
        }
        if (dto.password() == null || dto.password().isBlank()) {
            throw new IllegalArgumentException("password is required when creating a user.");
        }

        AppUser user = AppUser.builder()
                .name(dto.name())
                .username(dto.username())
                .phone(dto.phone())
                .address(dto.address())
                .email(dto.email())
                .passwordHash(passwordEncoder.encode(dto.password()))
                .build();

        AppUser saved = userRepository.save(user);
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public UserResponseDto update(Long userId, UserRequestDto dto) {
        AppUser existing = findOrThrow(userId);

        if (userRepository.existsByEmailAndIdNot(dto.email(), userId)) {
            throw new IllegalStateException("Email already registered: " + dto.email());
        }
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(dto.username(), userId)) {
            throw new IllegalStateException("Username already registered: " + dto.username());
        }

        existing.setName(dto.name());
        existing.setUsername(dto.username());
        existing.setPhone(dto.phone());
        existing.setAddress(dto.address());
        existing.setEmail(dto.email());

        if (dto.password() != null && !dto.password().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(dto.password()));
        }

        return mapper.toResponseDto(existing);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getById(Long userId) {
        return mapper.toResponseDto(findOrThrow(userId));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAll() {
        return userRepository.findAll().stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Transactional
    public UserResponseDto activate(Long userId) {
        AppUser user = findOrThrow(userId);
        user.setStatus(UserStatus.ACTIVE);
        return mapper.toResponseDto(user);
    }

    /** Approves a public registration. Admin-created users are already ACTIVE. */
    @Transactional
    public UserResponseDto approve(Long userId) {
        AppUser user = findOrThrow(userId);
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("Only pending registrations can be approved.");
        }
        user.setStatus(UserStatus.ACTIVE);
        return mapper.toResponseDto(user);
    }

    @Transactional
    public UserResponseDto deactivate(Long userId) {
        AppUser user = findOrThrow(userId);
        user.setStatus(UserStatus.INACTIVE);
        return mapper.toResponseDto(user);
    }

    /** Rejecting a registration disables it; it remains visible for an audit trail. */
    @Transactional
    public UserResponseDto reject(Long userId) {
        AppUser user = findOrThrow(userId);
        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalStateException("Only pending registrations can be rejected.");
        }
        user.setStatus(UserStatus.INACTIVE);
        return mapper.toResponseDto(user);
    }

    @Transactional
    public UserResponseDto assignRole(Long userId, Long roleId) {
        AppUser user = findOrThrow(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        user.getRoles().add(role);
        return mapper.toResponseDto(user);
    }

    @Transactional
    public UserResponseDto removeRole(Long userId, Long roleId) {
        AppUser user = findOrThrow(userId);
        user.getRoles().removeIf(r -> r.getId().equals(roleId));
        return mapper.toResponseDto(user);
    }

    private AppUser findOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }
}
