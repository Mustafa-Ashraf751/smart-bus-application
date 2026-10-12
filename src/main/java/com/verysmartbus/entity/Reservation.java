package com.verysmartbus.entity;

import com.verysmartbus.entity.enums.ReservationSource;
import com.verysmartbus.entity.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Uniqueness (one RESERVED/WAITLISTED reservation per user per trip) is enforced by the
 * partial unique index uq_reservation_user_trip_active, which JPA cannot express here.
 */
@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservation_user")
    )
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "trip_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservation_trip")
    )
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "pickup_trip_station_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservation_pickup_trip_station")
    )
    private TripStation pickupTripStation;

    // Nullable only for legacy rows created before drop-off existed; the service always sets it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "dropoff_trip_station_id",
            foreignKey = @ForeignKey(name = "fk_reservation_dropoff_trip_station")
    )
    private TripStation dropoffTripStation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private ReservationSource source = ReservationSource.MANUAL;

    @CreationTimestamp
    @Column(name = "reserved_at", nullable = false, updatable = false)
    private LocalDateTime reservedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public void cancel() {
        if (this.status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Reservation is already cancelled");
        }

        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    /** Waitlist -> confirmed seat, used when a RESERVED reservation on the same trip is cancelled. */
    public void promote() {
        if (this.status != ReservationStatus.WAITLISTED) {
            throw new IllegalStateException("Only a waitlisted reservation can be promoted");
        }

        this.status = ReservationStatus.RESERVED;
    }
}
