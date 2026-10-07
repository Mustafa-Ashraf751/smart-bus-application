package com.verysmartbus.seeders;



 import com.verysmartbus.dto.request.BusRequestDto;
 import com.verysmartbus.dto.request.UserRequestDto;
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
         if (userRepository.existsByEmail("admin@verysmartbus.com")) { log.info("Seeder: dev users already exist, skipping.");
             return; }
         userService.create(new UserRequestDto( "Admin User", "0100000000", "HQ", "admin@verysmartbus.com", "password123" ));
         userService.create(new UserRequestDto( "Test Driver", "0100000001", "Depot", "driver@verysmartbus.com", "password123" ));
         userService.create(new UserRequestDto( "Test Employee", "0100000002", "Office", "employee@verysmartbus.com", "password123" ));
       //  userService.create(new UserRequestDto( "Test BusAdmin", "0100000003", "MS", "BusAdmin@verysmartbus.com", "password123" ));
         assignRole( "admin@verysmartbus.com", "ADMIN" );
         assignRole( "driver@verysmartbus.com", "DRIVER" );
         assignRole( "employee@verysmartbus.com", "EMPLOYEE" );
         log.info("Seeder: created 3 dev users and assigned their roles.");
     }
     private void assignRole(String email, String roleName)
     {
         Long userId = userRepository.findByEmail(email) .orElseThrow(() -> new IllegalStateException( "User not found: " + email ) ) .getId();
         Long roleId = roleRepository.findByName(roleName) .orElseThrow(() -> new IllegalStateException( "Role not found: " + roleName ) ) .getId();
         userService.assignRole(userId, roleId); log.info( "Seeder: assigned role {} to user {}", roleName, email );
     }
     private void seedBuses() {
         if (busRepository.existsByBusNumber("BUS-001")) { log.info("Seeder: dev buses already exist, skipping.");
             return;
         }
         busService.create( new BusRequestDto("BUS-001", 40) );
         busService.create( new BusRequestDto("BUS-002", 30) );
         log.info("Seeder: created 2 dev buses.");
     }
 }

