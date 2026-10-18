package com.verysmartbus.entity;

import com.verysmartbus.entity.enums.NotificationOutboxStatus;
import com.verysmartbus.entity.enums.NotificationDeliveryChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notification_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_outbox_notification"))
    private Notification notification;

    @Column(name = "event_key", nullable = false, length = 200)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_channel", nullable = false, length = 20)
    @Builder.Default
    private NotificationDeliveryChannel deliveryChannel = NotificationDeliveryChannel.WEBSOCKET;

    /** Snapshot the address used for an email job so a later user-profile edit cannot redirect it. */
    @Column(name = "recipient_email", length = 150)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationOutboxStatus status = NotificationOutboxStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public void claim(OffsetDateTime claimedAt) {
        this.status = NotificationOutboxStatus.PROCESSING;
        this.lockedAt = claimedAt;
    }

    public void markSent(OffsetDateTime sentAt) {
        this.status = NotificationOutboxStatus.SENT;
        this.sentAt = sentAt;
        this.lockedAt = null;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.status = NotificationOutboxStatus.FAILED;
        this.lockedAt = null;
        this.lastError = error;
    }

    public void scheduleRetry(OffsetDateTime nextAttemptAt, String error) {
        this.status = NotificationOutboxStatus.PENDING;
        this.attemptCount++;
        this.nextAttemptAt = nextAttemptAt;
        this.lockedAt = null;
        this.lastError = error;
    }
}
