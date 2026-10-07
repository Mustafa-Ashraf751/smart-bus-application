package com.verysmartbus.entity;

import com.verysmartbus.entity.enums.RouteStatus;
import com.verysmartbus.entity.enums.Direction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.LineString;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Direction direction;

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(name = "path", columnDefinition = "extensions.geometry(LineString,4326)")
    private LineString path;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RouteStatus status = RouteStatus.ACTIVE;

    @Column(name = "default_departure_time")
    private LocalTime defaultDepartureTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_bus_id", foreignKey = @ForeignKey(name = "fk_routes_default_bus"))
    private Bus defaultBus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_driver_id", foreignKey = @ForeignKey(name = "fk_routes_default_driver"))
    private AppUser defaultDriver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_bus_admin_id", foreignKey = @ForeignKey(name = "fk_routes_default_bus_admin"))
    private AppUser defaultBusAdmin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
