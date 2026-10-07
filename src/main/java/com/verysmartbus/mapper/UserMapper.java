package com.verysmartbus.mapper;

import com.verysmartbus.dto.response.UserResponseDto;
import com.verysmartbus.entity.AppUser;
import com.verysmartbus.entity.Role;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponseDto toResponseDto(AppUser entity) {
        Set<String> roleNames = entity.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new UserResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getEmail(),
                entity.getStatus(),
                roleNames,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
