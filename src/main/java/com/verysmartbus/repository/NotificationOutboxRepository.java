package com.verysmartbus.repository;

import com.verysmartbus.entity.NotificationOutbox;
import com.verysmartbus.entity.enums.NotificationOutboxStatus;
import com.verysmartbus.entity.enums.NotificationDeliveryChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    Optional<NotificationOutbox> findByEventKeyAndDeliveryChannel(
            String eventKey,
            NotificationDeliveryChannel deliveryChannel
    );

    List<NotificationOutbox> findAllByDeliveryChannelAndStatusAndLockedAtBefore(
            NotificationDeliveryChannel deliveryChannel,
            NotificationOutboxStatus status,
            OffsetDateTime lockedBefore
    );

    /**
     * PostgreSQL locks each returned row for this transaction. Other workers skip those locks and
     * claim different rows, which lets multiple application instances safely process the outbox.
     */
    @Query(value = """
            SELECT *
            FROM notification_outbox
            WHERE status = 'PENDING'
              AND delivery_channel = :deliveryChannel
              AND next_attempt_at <= CURRENT_TIMESTAMP
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<NotificationOutbox> lockPendingBatch(
            @Param("deliveryChannel") String deliveryChannel,
            @Param("batchSize") int batchSize
    );
}
