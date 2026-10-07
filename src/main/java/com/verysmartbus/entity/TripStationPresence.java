package com.verysmartbus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "trip_station_presence",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_trip_station_presence_trip_station",
                columnNames = {"trip_id", "station_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripStationPresence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_station_presence_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "trip_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_trip_station_presence_trip")
    )
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "station_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_trip_station_presence_station")
    )
    private Station station;

    @Builder.Default
    @Column(name = "consecutive_inside_count", nullable = false)
    private Integer consecutiveInsideCount = 0;

    @Builder.Default
    @Column(name = "arrival_confirmed", nullable = false)
    private boolean arrivalConfirmed = false;

    @Column(name = "last_observed_at")
    private OffsetDateTime lastObservedAt;
}
