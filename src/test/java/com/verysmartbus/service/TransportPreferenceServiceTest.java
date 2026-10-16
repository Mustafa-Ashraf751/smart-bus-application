package com.verysmartbus.service;

import com.verysmartbus.dto.response.RouteResponseDto;
import com.verysmartbus.dto.response.RouteStationResponseDto;
import com.verysmartbus.entity.Route;
import com.verysmartbus.entity.RouteStation;
import com.verysmartbus.entity.Station;
import com.verysmartbus.entity.enums.RouteStatus;
import com.verysmartbus.entity.enums.StationStatus;
import com.verysmartbus.mapper.RouteMapper;
import com.verysmartbus.mapper.RouteStationMapper;
import com.verysmartbus.mapper.TransportPreferenceMapper;
import com.verysmartbus.repository.RouteRepository;
import com.verysmartbus.repository.RouteStationRepository;
import com.verysmartbus.repository.TransportPreferenceRepository;
import com.verysmartbus.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransportPreferenceServiceTest {

    private final RouteRepository routeRepository = mock(RouteRepository.class);
    private final RouteStationRepository routeStationRepository = mock(RouteStationRepository.class);
    private final RouteMapper routeMapper = mock(RouteMapper.class);
    private final RouteStationMapper routeStationMapper = mock(RouteStationMapper.class);
    private final TransportPreferenceService service = new TransportPreferenceService(
            mock(TransportPreferenceRepository.class), mock(UserRepository.class), routeRepository,
            routeStationRepository, mock(TransportPreferenceMapper.class), routeMapper, routeStationMapper);

    @Test
    void preferenceRoutePickerReturnsOnlyRepositorySelectedActiveRoutes() {
        Route route = Route.builder().id(1L).status(RouteStatus.ACTIVE).build();
        RouteResponseDto response = mock(RouteResponseDto.class);
        when(routeRepository.findAllByStatusOrderByNameAsc(RouteStatus.ACTIVE)).thenReturn(List.of(route));
        when(routeMapper.toResponseDto(route)).thenReturn(response);

        assertEquals(List.of(response), service.getActiveRoutesForPreference());
        verify(routeRepository).findAllByStatusOrderByNameAsc(RouteStatus.ACTIVE);
    }

    @Test
    void preferenceStationPickerExcludesInactivePhysicalStations() {
        Route route = Route.builder().id(1L).status(RouteStatus.ACTIVE).build();
        RouteStation active = RouteStation.builder().route(route)
                .station(Station.builder().status(StationStatus.ACTIVE).build()).build();
        RouteStation inactive = RouteStation.builder().route(route)
                .station(Station.builder().status(StationStatus.INACTIVE).build()).build();
        RouteStationResponseDto response = mock(RouteStationResponseDto.class);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeStationRepository.findAllByRoute_IdOrderByStopOrderAsc(1L)).thenReturn(List.of(active, inactive));
        when(routeStationMapper.toResponseDto(active)).thenReturn(response);

        assertEquals(List.of(response), service.getActiveRouteStationsForPreference(1L));
        verify(routeStationMapper).toResponseDto(active);
    }
}
