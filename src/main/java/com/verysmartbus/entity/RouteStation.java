package com.verysmartbus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "route_stations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_route_stations_route_station", columnNames = {"route_id", "station_id"}),
                @UniqueConstraint(name = "uq_route_stations_route_order", columnNames = {"route_id", "stop_order"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "route_id", nullable = false, foreignKey = @ForeignKey(name = "fk_route_stations_route"))
    private Route route;

    @ManyToOne(optional = false)
    @JoinColumn(name = "station_id", nullable = false, foreignKey = @ForeignKey(name = "fk_route_stations_station"))
    private Station station;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @Column(name = "expected_arrival_time", nullable = false)
    private LocalTime expectedArrivalTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
