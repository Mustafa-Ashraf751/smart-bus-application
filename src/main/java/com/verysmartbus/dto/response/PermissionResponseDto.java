package com.verysmartbus.dto.response;

public record PermissionResponseDto(
        Long permissionId,
        String name,
        String description
) {
}
