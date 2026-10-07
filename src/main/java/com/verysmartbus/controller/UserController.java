package com.verysmartbus.controller;
import com.verysmartbus.dto.request.UserRequestDto;
import com.verysmartbus.dto.response.UserResponseDto;
import com.verysmartbus.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @PostMapping
    public ResponseEntity<UserResponseDto> create(@Valid @RequestBody UserRequestDto dto) {
        UserResponseDto created = userService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{userId}")
                .buildAndExpand(created.userId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
    @GetMapping
    public List<UserResponseDto> getAll() {
        return userService.getAll();
    }@GetMapping("/{userId}")
    public UserResponseDto getById(@PathVariable Long userId) {
        return userService.getById(userId);
    }
    @PutMapping("/{userId}")
    public UserResponseDto update(@PathVariable Long userId,
                                  @Valid @RequestBody UserRequestDto dto) {
        return userService.update(userId, dto);
    }
    @PatchMapping("/{userId}/activate")
    public UserResponseDto activate(@PathVariable Long userId) {
        return userService.activate(userId);
    }
    @PatchMapping("/{userId}/deactivate")
    public UserResponseDto deactivate(@PathVariable Long userId) {
        return userService.deactivate(userId);
    }
    @PutMapping("/{userId}/roles/{roleId}")
    public UserResponseDto assignRole(@PathVariable Long userId,
                                      @PathVariable Long roleId) {
        return userService.assignRole(userId, roleId);
    }
    @DeleteMapping("/{userId}/roles/{roleId}")
    public UserResponseDto removeRole(@PathVariable Long userId,
                                      @PathVariable Long roleId) {
        return userService.removeRole(userId, roleId);
    }
}
