package com.example.lab.payment.kafka;

import com.example.lab.payment.domain.Payment;
import com.example.lab.payment.domain.kafka.PaymentEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class PaymentEventPublisher {
    public PaymentEvent createEvent(Payment payment, String eventType) {
        return new PaymentEvent(UUID.randomUUID(), eventType, payment.getId(),
                payment.getGatewayReference(), payment.getAmount(), payment.getCurrency(),
                payment.getStatus().name(), payment.getAttempt(), Instant.now(), payment.getSagaId(),
                payment.getInventoryId(), payment.getInventoryQuantity() == null ? 0L : payment.getInventoryQuantity(), payment.getSagaFailureMode());
    }
}
