package com.verysmartbus.service;

import com.verysmartbus.dto.request.BusRequestDto;
import com.verysmartbus.dto.response.BusResponseDto;
import com.verysmartbus.entity.Bus;
import com.verysmartbus.entity.Bus.BusStatus;
import com.verysmartbus.mapper.BusMapper;
import com.verysmartbus.repository.BusRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusService {

    private final BusRepository busRepository;
    private final BusMapper mapper;

    @Transactional
    public BusResponseDto create(BusRequestDto dto) {

        if (busRepository.existsByBusNumber(dto.busNumber())) {
            throw new IllegalStateException("Bus number already exists: " + dto.busNumber());
        }

        Bus saved = busRepository.save(mapper.toEntity(dto));
        return mapper.toResponseDto(saved);
    }

    @Transactional
    public BusResponseDto update(Long id, BusRequestDto dto) {
        Bus existing = findOrThrow(id);


        if (busRepository.existsByBusNumberAndIdNot(dto.busNumber(), id)) {
            throw new IllegalStateException("Bus number already exists: " + dto.busNumber());
        }

        mapper.updateEntity(existing, dto);
        return mapper.toResponseDto(existing);
    }

    @Transactional(readOnly = true)
    public BusResponseDto getById(Long id) {
        return mapper.toResponseDto(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<BusResponseDto> getAll(BusStatus status) {
        List<Bus> buses = (status == null)
                ? busRepository.findAll()
                : busRepository.findByStatus(status);
        return buses.stream().map(mapper::toResponseDto).toList();
    }

    @Transactional
    public BusResponseDto activate(Long id) {
        Bus bus = findOrThrow(id);
        bus.setStatus(BusStatus.ACTIVE);
        return mapper.toResponseDto(bus);
    }


    @Transactional
    public BusResponseDto deactivate(Long id) {
        Bus bus = findOrThrow(id);
        bus.setStatus(BusStatus.INACTIVE);
        return mapper.toResponseDto(bus);
    }

    private Bus findOrThrow(Long id) {
        return busRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bus not found: " + id));
    }
}
