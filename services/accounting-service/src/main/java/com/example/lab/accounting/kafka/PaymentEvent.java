package com.example.lab.accounting.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentEvent(UUID eventId, String eventType, UUID paymentId, String gatewayReference,
                           BigDecimal amount, String currency, String status, int attempt, Instant occurredAt) {}
