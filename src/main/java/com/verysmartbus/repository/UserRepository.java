package com.verysmartbus.repository;

import com.verysmartbus.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findById(long _id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appUser from AppUser appUser where appUser.id = :userId")
    Optional<AppUser> findByIdForUpdate(@Param("userId") Long userId);
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
            where lower(appUser.email) = lower(:identifier)
               or lower(appUser.username) = lower(:identifier)
            """)
    Optional<AppUser> findByLoginIdentifierWithRolesAndPermissions(@Param("identifier") String identifier);

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

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
}
