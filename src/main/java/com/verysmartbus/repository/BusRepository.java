package com.verysmartbus.repository;

import com.verysmartbus.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusRepository extends JpaRepository<Bus, Long> {

    Optional<Bus> findByBusNumber(String busNumber);

    boolean existsByBusNumber(String busNumber);

    boolean existsByBusNumberAndIdNot(String busNumber, Long id);

    List<Bus> findByStatus(Bus.BusStatus status);
}
