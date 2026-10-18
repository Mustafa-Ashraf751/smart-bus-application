package com.verysmartbus.service;

import com.verysmartbus.dto.request.UserRequestDto;
import com.verysmartbus.dto.response.UserResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.mapper.UserMapper;
import com.verysmartbus.repository.RoleRepository;
import com.verysmartbus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void adminCreationPersistsTheUsername() {
        UserRequestDto request = new UserRequestDto(
                "Sara Ahmed", "sara.ahmed", "01000000000", "Cairo",
                "sara@example.com", "password123");
        AppUser saved = AppUser.builder().id(15L).username("sara.ahmed").build();
        UserResponseDto expected = org.mockito.Mockito.mock(UserResponseDto.class);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(request.username())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(AppUser.class))).thenReturn(saved);
        when(userMapper.toResponseDto(saved)).thenReturn(expected);

        assertThat(userService.create(request)).isSameAs(expected);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        org.mockito.Mockito.verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("sara.ahmed");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    void adminUpdateChangesTheUsernameWhenItIsAvailable() {
        AppUser existing = AppUser.builder()
                .id(15L)
                .username("sara.old")
                .email("sara@example.com")
                .build();
        UserRequestDto request = new UserRequestDto(
                "Sara Ahmed", "sara.new", "01000000000", "Cairo",
                "sara@example.com", null);
        UserResponseDto expected = org.mockito.Mockito.mock(UserResponseDto.class);

        when(userRepository.findById(Long.valueOf(15L))).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot(request.email(), 15L)).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCaseAndIdNot(request.username(), 15L)).thenReturn(false);
        when(userMapper.toResponseDto(existing)).thenReturn(expected);

        assertThat(userService.update(15L, request)).isSameAs(expected);
        assertThat(existing.getUsername()).isEqualTo("sara.new");
    }
}
