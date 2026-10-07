package com.verysmartbus.seeders;


import com.verysmartbus.dto.request.PermissionRequestDto;
import com.verysmartbus.dto.request.RoleRequestDto;
import com.verysmartbus.dto.response.RoleResponseDto;
import com.verysmartbus.repository.PermissionRepository;
import com.verysmartbus.repository.RoleRepository;
import com.verysmartbus.repository.UserRepository;
import com.verysmartbus.service.PermissionService;
import com.verysmartbus.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataSeeder {

    private final RoleService roleService;
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    private static final Map<String, String> PERMISSIONS = Map.of(
            "MANAGE_USERS", "Create, update, deactivate users",
            "MANAGE_ROLES", "Create, update roles and their permissions",
            "MANAGE_TRIPS", "Create and schedule trips",
            "MANAGE_BUSES", "Create, update, deactivate buses",
            "VIEW_ALL_RESERVATIONS", "View reservations for any trip, not just your own",
            "BOOK_TRIP", "Reserve a seat on a trip",
            "UPDATE_LIVE_LOCATION", "Push live GPS location for an in-progress trip"
    );

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            "ADMIN", List.of(), // filled with ALL permissions below
            "DRIVER", List.of("UPDATE_LIVE_LOCATION"),
            "EMPLOYEE", List.of("BOOK_TRIP")
    );

    public void seed() {
        if (roleRepository.existsByName("ADMIN")) {
            log.info("Seeder: reference data (roles/permissions) already present, skipping.");
            return;
        }

        PERMISSIONS.forEach((name, description) ->
                permissionService.create(new PermissionRequestDto(name, description)));

        RoleResponseDto admin = roleService.create(new RoleRequestDto("ADMIN", "Full access to manage the system"));
        RoleResponseDto driver = roleService.create(new RoleRequestDto("DRIVER", "Can operate trips and update live location"));
        RoleResponseDto employee = roleService.create(new RoleRequestDto("EMPLOYEE", "Regular employee who books bus trips"));

        // ADMIN gets every permission.
        permissionRepository.findAll().forEach(p -> roleService.assignPermission(admin.roleId(), p.getId()));

        ROLE_PERMISSIONS.get("DRIVER").forEach(permName ->
                permissionRepository.findByName(permName)
                        .ifPresent(p -> roleService.assignPermission(driver.roleId(), p.getId())));

        ROLE_PERMISSIONS.get("EMPLOYEE").forEach(permName ->
                permissionRepository.findByName(permName)
                        .ifPresent(p -> roleService.assignPermission(employee.roleId(), p.getId())));

        log.info("Seeder: created ADMIN, DRIVER, EMPLOYEE roles with {} permissions.", PERMISSIONS.size());
    }
}
