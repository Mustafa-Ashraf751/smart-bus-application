package com.verysmartbus.controller;
import com.verysmartbus.dto.request.PermissionRequestDto;
import com.verysmartbus.dto.response.PermissionResponseDto;
import com.verysmartbus.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;
@RestController
@RequestMapping("/api/admin/permissions")
public class PermissionController {
    private final PermissionService permissionService;
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
    @PostMapping
    public ResponseEntity<PermissionResponseDto> create(@Valid @RequestBody PermissionRequestDto dto) {
        PermissionResponseDto created = permissionService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{permissionId}")
                .buildAndExpand(created.permissionId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
    @GetMapping
    public List<PermissionResponseDto> getAll() {
        return permissionService.getAll();
    }
    @GetMapping("/{permissionId}")
    public PermissionResponseDto getById(@PathVariable("permissionId") Long permissionId) {
        return permissionService.getById(permissionId);
    }
    @PutMapping("/{permissionId}")
    public PermissionResponseDto update(@PathVariable("permissionId") Long permissionId,
                                        @Valid @RequestBody PermissionRequestDto dto) {
        return permissionService.update(permissionId, dto);
    }
    @DeleteMapping("/{permissionId}")
    public ResponseEntity<Void> delete(@PathVariable("permissionId") Long permissionId) {
        permissionService.delete(permissionId);
        return ResponseEntity.noContent().build();
    }
}
