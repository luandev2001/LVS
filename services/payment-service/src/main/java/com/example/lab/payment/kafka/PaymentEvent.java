package com.example.lab.payment.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(
        UUID eventId, String eventType, UUID paymentId, String gatewayReference,
        BigDecimal amount, String currency, String status, int attempt, Instant occurredAt,
        UUID sagaId, Long inventoryId, long quantity, String sagaFailureMode) {
    public PaymentEvent(UUID eventId, String eventType, UUID paymentId, String gatewayReference,
                        BigDecimal amount, String currency, String status, int attempt, Instant occurredAt) {
        this(eventId, eventType, paymentId, gatewayReference, amount, currency, status, attempt, occurredAt, null, null, 0L, "NONE");
    }
}
