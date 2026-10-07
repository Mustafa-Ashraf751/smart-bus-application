package com.verysmartbus.entity;

import com.verysmartbus.entity.enums.TripStatus;
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
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false, foreignKey = @ForeignKey(name = "fk_trips_route"))
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bus_id", nullable = false, foreignKey = @ForeignKey(name = "fk_trips_bus"))
    private Bus bus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false, foreignKey = @ForeignKey(name = "fk_trips_driver"))
    private AppUser driver;

    @Column(name = "scheduled_start_time", nullable = false)
    private OffsetDateTime scheduledStartTime;

    @Column(name = "actual_start_time")
    private OffsetDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private OffsetDateTime actualEndTime;

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(name = "current_location", columnDefinition = "extensions.geometry(Point,4326)")
    private Point currentLocation;

    @Column(name = "location_updated_at")
    private OffsetDateTime locationUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TripStatus status = TripStatus.SCHEDULED;

    @Column(name = "active_date")
    private LocalDate activeDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * لو آخر مرة اتسجلت فيها حالة تشغيلية للرحلة دي مش النهاردة،
     * يصفّر كل حاجة خاصة باليوم (الحالة، الأوقات الفعلية، الموقع)
     * استعدادًا ليوم جديد. بترجع true لو فعلاً حصل تصفير.
     */
    public boolean resetIfStale() {
        LocalDate today = LocalDate.now();
        if (this.activeDate == null || !this.activeDate.equals(today)) {
            this.status = TripStatus.SCHEDULED;
            this.actualStartTime = null;
            this.actualEndTime = null;
            this.currentLocation = null;
            this.locationUpdatedAt = null;
            this.activeDate = today;
            return true;
        }
        return false;
    }
}