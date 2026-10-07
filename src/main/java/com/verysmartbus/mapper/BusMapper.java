package com.verysmartbus.mapper;

import com.verysmartbus.dto.request.BusRequestDto;
import com.verysmartbus.dto.response.BusResponseDto;
import com.verysmartbus.entity.Bus;
import org.springframework.stereotype.Component;

@Component
public class BusMapper {

    public Bus toEntity(BusRequestDto dto) {
        return Bus.builder()
                .busNumber(dto.busNumber())
                .capacity(dto.capacity())
                .build();
    }

    public void updateEntity(Bus entity, BusRequestDto dto) {
        entity.setBusNumber(dto.busNumber());
        entity.setCapacity(dto.capacity());
    }

    public BusResponseDto toResponseDto(Bus entity) {
        return new BusResponseDto(
                entity.getId(),
                entity.getBusNumber(),
                entity.getCapacity(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
