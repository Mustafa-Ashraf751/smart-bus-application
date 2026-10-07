package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.PermissionRequestDto;
import com.verysmartbus.dto.response.PermissionResponseDto;
import com.verysmartbus.entity.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public Permission toEntity(PermissionRequestDto dto) {
        return Permission.builder()
                .name(dto.name())
                .description(dto.description())
                .build();
    }

    public void updateEntity(Permission entity, PermissionRequestDto dto) {
        entity.setName(dto.name());
        entity.setDescription(dto.description());
    }

    public PermissionResponseDto toResponseDto(Permission entity) {
        return new PermissionResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getDescription()
        );
    }
}
