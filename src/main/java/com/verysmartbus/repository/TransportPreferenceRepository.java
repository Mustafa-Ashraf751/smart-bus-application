package com.verysmartbus.repository;

import com.verysmartbus.entity.enums.Direction;
import com.verysmartbus.entity.TransportPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransportPreferenceRepository extends JpaRepository<TransportPreference, Long> {

    List<TransportPreference> findByUser_Id(Long Id);

    boolean existsByUser_IdAndDirection(Long Id, Direction direction);
}
