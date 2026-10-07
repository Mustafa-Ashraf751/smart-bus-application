package com.verysmartbus.dto.response;

import java.util.Set;

public record RoleResponseDto(
        Long roleId,
        String name,
        String description,
        Set<String> permissionNames
) {
}
