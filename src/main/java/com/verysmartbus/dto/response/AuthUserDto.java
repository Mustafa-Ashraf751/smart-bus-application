package com.verysmartbus.dto.response;

import com.verysmartbus.entity.enums.UserStatus;
import com.verysmartbus.security.UserPrincipal;

import java.util.List;

/**
 * The logged-in user as the frontend needs it: identity, roles and permissions.
 * Same data as the JWT claims, returned on login and by GET /api/auth/me.
 */
public record AuthUserDto(
        Long userId,
        String name,
        String username,
        String email,
        String phone,
        String address,
        UserStatus status,
        List<String> roles,
        List<String> permissions
) {
    public static AuthUserDto from(UserPrincipal principal) {
        return new AuthUserDto(
                principal.getUserId(),
                principal.getName(),
                principal.getLoginUsername(),
                principal.getUsername(),
                principal.getPhone(),
                principal.getAddress(),
                principal.getStatus(),
                principal.getRoleNames(),
                principal.getPermissionNames()
        );
    }
}
