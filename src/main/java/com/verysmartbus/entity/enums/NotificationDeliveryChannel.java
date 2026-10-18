package com.verysmartbus.entity.enums;

/** A durable outbox job is processed by exactly one delivery mechanism. */
public enum NotificationDeliveryChannel {
    WEBSOCKET,
    EMAIL
}
