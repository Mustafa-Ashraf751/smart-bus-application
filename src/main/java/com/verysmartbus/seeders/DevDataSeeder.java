package com.verysmartbus.seeders;

import com.verysmartbus.dto.request.BusRequestDto;
import com.verysmartbus.dto.request.UserRequestDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.repository.BusRepository;
import com.verysmartbus.repository.RoleRepository;
import com.verysmartbus.repository.UserRepository;
import com.verysmartbus.service.BusService;
import com.verysmartbus.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BusService busService;
    private final BusRepository busRepository;

    public void seed() {
        seedUsers();
        seedBuses();
    }

    private void seedUsers() {
        seedUser("Admin User", "admin", "0100000000", "HQ", "admin@verysmartbus.com", "ADMIN");
        seedUser("Test Driver", "driver", "0100000001", "Depot", "driver@verysmartbus.com", "DRIVER");
        seedUser("Test Employee", "employee", "0100000002", "Office", "employee@verysmartbus.com", "EMPLOYEE");
        seedUser("Test Bus Admin", "busadmin", "0100000003", "Operations", "busadmin@verysmartbus.com", "BUS_ADMIN");
    }

    private void seedUser(String name, String username, String phone, String address, String email, String roleName) {
        if (!userRepository.existsByEmail(email)) {
            userService.create(new UserRequestDto(name, username, phone, address, email, "password123"));
            log.info("Seeder: created dev user {}.", email);
        }
        assignRoleIfMissing(email, roleName);
    }

    private void assignRoleIfMissing(String email, String roleName) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        AppUser userWithRoles = userRepository.findByIdWithRoles(user.getId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        boolean alreadyAssigned = userWithRoles.getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getName()));
        if (alreadyAssigned) {
            return;
        }

        Long roleId = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName))
                .getId();
        userService.assignRole(user.getId(), roleId);
        log.info("Seeder: assigned role {} to user {}.", roleName, email);
    }

    private void seedBuses() {
        if (busRepository.existsByBusNumber("BUS-001")) {
            log.info("Seeder: dev buses already exist, skipping.");
            return;
        }
        busService.create(new BusRequestDto("BUS-001", 40));
        busService.create(new BusRequestDto("BUS-002", 30));
        log.info("Seeder: created 2 dev buses.");
    }
}

