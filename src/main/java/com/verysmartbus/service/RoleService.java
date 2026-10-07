package com.verysmartbus.service;

import com.verysmartbus.dto.request.RoleRequestDto;
import com.verysmartbus.dto.response.RoleResponseDto;
import com.verysmartbus.entity.Permission;
import com.verysmartbus.entity.Role;
import com.verysmartbus.mapper.RoleMapper;
import com.verysmartbus.repository.PermissionRepository;
import com.verysmartbus.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper mapper;

    @Transactional
    public RoleResponseDto create(RoleRequestDto dto) {
        if (roleRepository.existsByName(dto.name())) {
            throw new IllegalStateException("Role name already exists: " + dto.name());
        }
        Role saved = roleRepository.save(mapper.toEntity(dto));
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public RoleResponseDto update(Long roleId, RoleRequestDto dto) {
        Role existing = findOrThrow(roleId);

        if (roleRepository.existsByNameAndIdNot(dto.name(), roleId)) {
            throw new IllegalStateException("Role name already exists: " + dto.name());
        }

        mapper.updateEntity(existing, dto);
        return mapper.toResponseDto(existing);
    }

    @Transactional(readOnly = true)
    public RoleResponseDto getById(Long roleId) {
        return mapper.toResponseDto(findOrThrow(roleId));
    }

    @Transactional(readOnly = true)
    public List<RoleResponseDto> getAll() {
        return roleRepository.findAll().stream()
                .map(mapper::toResponseDto)
                .toList();
    }

    @Transactional
    public void delete(Long roleId) {
        roleRepository.delete(findOrThrow(roleId));
    }


    @Transactional
    public RoleResponseDto assignPermission(Long roleId, Long permissionId) {
        Role role = findOrThrow(roleId);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + permissionId));

        role.getPermissions().add(permission);
        return mapper.toResponseDto(role);
    }

    @Transactional
    public RoleResponseDto removePermission(Long roleId, Long permissionId) {
        Role role = findOrThrow(roleId);
        role.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        return mapper.toResponseDto(role);
    }

    private Role findOrThrow(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));
    }
}
