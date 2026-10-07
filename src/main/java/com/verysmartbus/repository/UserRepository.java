package com.verysmartbus.repository;

import com.verysmartbus.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findById(long _id);
    Optional<AppUser> findByEmail(String email);

    @Query("""
            select distinct appUser
            from AppUser appUser
            left join fetch appUser.roles
            where appUser.id = :userId
            """)
    Optional<AppUser> findByIdWithRoles(@Param("userId") Long userId);

    @Query("""
            select distinct appUser
            from AppUser appUser
            left join fetch appUser.roles appRole
            left join fetch appRole.permissions
            where appUser.email = :email
            """)
    Optional<AppUser> findByEmailWithRolesAndPermissions(@Param("email") String email);

    @Query("""
            select distinct appUser
            from AppUser appUser
            left join fetch appUser.roles appRole
            left join fetch appRole.permissions
            where appUser.id = :userId
            """)
    Optional<AppUser> findByIdWithRolesAndPermissions(@Param("userId") Long userId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
