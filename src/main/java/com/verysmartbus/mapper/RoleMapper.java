package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.RoleRequestDto;
import com.verysmartbus.dto.response.RoleResponseDto;
import com.verysmartbus.entity.Permission;
import com.verysmartbus.entity.Role;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RoleMapper {

    public Role toEntity(RoleRequestDto dto) {
        return Role.builder()
                .name(dto.name())
                .description(dto.description())
                .build();
    }

    public void updateEntity(Role entity, RoleRequestDto dto) {
        entity.setName(dto.name());
        entity.setDescription(dto.description());
    }

    public RoleResponseDto toResponseDto(Role entity) {
        Set<String> permissionNames = entity.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        return new RoleResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                permissionNames
        );
    }
}
