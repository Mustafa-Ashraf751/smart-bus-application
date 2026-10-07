package com.verysmartbus.service;

import com.verysmartbus.dto.request.PermissionRequestDto;
import com.verysmartbus.dto.response.PermissionResponseDto;
import com.verysmartbus.entity.Permission;
import com.verysmartbus.mapper.PermissionMapper;
import com.verysmartbus.repository.PermissionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper mapper;

    @Transactional
    public PermissionResponseDto create(PermissionRequestDto dto) {
        if (permissionRepository.existsByName(dto.name())) {
            throw new IllegalStateException("Permission name already exists: " + dto.name());
        }
        Permission saved = permissionRepository.save(mapper.toEntity(dto));
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public PermissionResponseDto update(Long permissionId, PermissionRequestDto dto) {
        Permission existing = findOrThrow(permissionId);

        if (permissionRepository.existsByNameAndIdNot(dto.name(), permissionId)) {
            throw new IllegalStateException("Permission name already exists: " + dto.name());
        }

        mapper.updateEntity(existing, dto);
        return mapper.toResponseDto(existing);
    }

    @Transactional(readOnly = true)
    public PermissionResponseDto getById(Long permissionId) {
        return mapper.toResponseDto(findOrThrow(permissionId));
    }

    @Transactional(readOnly = true)
    public List<PermissionResponseDto> getAll() {
        return permissionRepository.findAll().stream()
                .map(mapper::toResponseDto)
                .toList();
    }


    @Transactional
    public void delete(Long permissionId) {
        Permission existing = findOrThrow(permissionId);
        permissionRepository.delete(existing);
    }

    private Permission findOrThrow(Long permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found: " + permissionId));
    }
}
