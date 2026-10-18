package com.verysmartbus.security;

import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Spring Security's representation of one authenticated application user.
 */
public class UserPrincipal implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_";

    private final Long userId;
    private final String email;
    private final String loginUsername;
    private final String name;
    private final String phone;
    private final String address;
    private final UserStatus status;
    private final String passwordHash;
    private final Set<GrantedAuthority> authorities;
    private final List<String> roleNames;
    private final List<String> permissionNames;

    private UserPrincipal(
            Long userId,
            String email,
            String loginUsername,
            String name,
            String phone,
            String address,
            UserStatus status,
            String passwordHash,
            Set<String> roleNames,
            Set<String> permissionNames
    ) {
        this.userId = userId;
        this.email = email;
        this.loginUsername = loginUsername;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.status = status;
        this.passwordHash = passwordHash;
        this.roleNames = List.copyOf(roleNames);
        this.permissionNames = List.copyOf(permissionNames);

        Set<GrantedAuthority> grantedAuthorities = new LinkedHashSet<>();
        roleNames.forEach(roleName -> grantedAuthorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + roleName)));
        permissionNames.forEach(permissionName -> grantedAuthorities.add(new SimpleGrantedAuthority(permissionName)));
        this.authorities = Set.copyOf(grantedAuthorities);
    }

    public static UserPrincipal from(AppUser user) {
        Set<String> roleNames = new TreeSet<>();
        Set<String> permissionNames = new TreeSet<>();

        user.getRoles().forEach(role -> {
            roleNames.add(role.getName());
            role.getPermissions().forEach(permission -> permissionNames.add(permission.getName()));
        });

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getName(),
                user.getPhone(),
                user.getAddress(),
                user.getStatus(),
                user.getPasswordHash(),
                roleNames,
                permissionNames
        );
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getLoginUsername() {
        return loginUsername;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public UserStatus getStatus() {
        return status;
    }

    /** Role names without the ROLE_ prefix, e.g. ADMIN, DRIVER. */
    public List<String> getRoleNames() {
        return roleNames;
    }

    /** Permission names granted through the user's roles, e.g. BOOK_TRIP. */
    public List<String> getPermissionNames() {
        return permissionNames;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
