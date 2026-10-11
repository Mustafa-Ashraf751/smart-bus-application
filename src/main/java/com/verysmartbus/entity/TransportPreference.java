package com.verysmartbus.entity;

import com.verysmartbus.entity.enums.Direction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "transport_preference",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_transport_preference_user_direction",
                columnNames = {"user_id", "direction"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long preferenceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_transport_preference_user")
    )
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "route_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_transport_preference_route")
    )
    private Route route;

    // Nullable only until legacy rows (single station_id) are cleaned up; the service
    // always requires both on create/update. Tighten to NOT NULL in a later migration.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pickup_route_station_id",
            foreignKey = @ForeignKey(name = "fk_transport_preference_pickup_route_station")
    )
    private RouteStation pickupRouteStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "dropoff_route_station_id",
            foreignKey = @ForeignKey(name = "fk_transport_preference_dropoff_route_station")
    )
    private RouteStation dropoffRouteStation;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private Direction direction;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
