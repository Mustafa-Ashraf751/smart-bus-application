package com.verysmartbus.controller;

import com.verysmartbus.dto.request.LoginRequestDto;
import com.verysmartbus.dto.response.AuthResponseDto;
import com.verysmartbus.dto.response.AuthUserDto;
import com.verysmartbus.security.AuthService;
import com.verysmartbus.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    /** Current user with up-to-date roles and permissions (the token's copy may be older). */
    @GetMapping("/me")
    public AuthUserDto me(@AuthenticationPrincipal UserPrincipal principal) {
        return AuthUserDto.from(principal);
    }

}
