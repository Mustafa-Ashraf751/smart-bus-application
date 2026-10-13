package com.verysmartbus.repository;

import com.verysmartbus.entity.TransportPreference;
import com.verysmartbus.entity.enums.Direction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransportPreferenceRepository extends JpaRepository<TransportPreference, Long> {

    List<TransportPreference> findByUser_Id(Long userId);

    boolean existsByUser_IdAndDirection(Long userId, Direction direction);

    boolean existsByUser_IdAndDirectionAndPreferenceIdNot(Long userId, Direction direction, Long preferenceId);

    /** Active preferences of a route, oldest first: the order in which seats are handed out. */
    List<TransportPreference> findByRoute_IdAndActiveTrueOrderByCreatedAtAscPreferenceIdAsc(Long routeId);
}
