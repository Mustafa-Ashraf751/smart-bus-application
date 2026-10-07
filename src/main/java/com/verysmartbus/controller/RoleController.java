package com.verysmartbus.controller;
import com.verysmartbus.dto.request.RoleRequestDto;
import com.verysmartbus.dto.response.RoleResponseDto;
import com.verysmartbus.service.RoleService;
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
@RequestMapping("/api/admin/roles")
public class RoleController {
    private final RoleService roleService;
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }
    @PostMapping
    public ResponseEntity<RoleResponseDto> create(@Valid @RequestBody RoleRequestDto dto) {
        RoleResponseDto created = roleService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{roleId}")
                .buildAndExpand(created.roleId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
    @GetMapping
    public List<RoleResponseDto> getAll() {
        return roleService.getAll();
    }
    @GetMapping("/{roleId}")
    public RoleResponseDto getById(@PathVariable("roleId") Long roleId) {
        return roleService.getById(roleId);
    }
    @PutMapping("/{roleId}")
    public RoleResponseDto update(@PathVariable("roleId") Long roleId, @Valid @RequestBody RoleRequestDto dto) {
        return roleService.update(roleId, dto);
    }
    @PutMapping("/{roleId}/permissions/{permissionId}")
    public RoleResponseDto assignPermission(@PathVariable("roleId") Long roleId, @PathVariable("permissionId") Long permissionId) {
        return roleService.assignPermission(roleId, permissionId);
    }
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> delete(@PathVariable("roleId") Long roleId) {
        roleService.delete(roleId);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public RoleResponseDto removePermission(@PathVariable("roleId") Long roleId, @PathVariable("permissionId") Long permissionId) {
        return roleService.removePermission(roleId, permissionId);
    }
}
