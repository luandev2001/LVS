package com.example.lab.payment.outbox;

import com.example.lab.payment.domain.PaymentOutbox;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentOutboxStateTest {
    @Test
    void movesFromPendingToProcessingToPublished() {
        PaymentOutbox row = PaymentOutbox.pending(
                UUID.randomUUID(), "PAYMENT_PENDING", "payment.events.v1", "payment-1", "{}");

        assertEquals(PaymentOutbox.Status.PENDING, row.getStatus());
        row.markProcessing();
        assertEquals(PaymentOutbox.Status.PROCESSING, row.getStatus());
        assertEquals(1, row.getAttempts());

        row.markPublished();
        assertEquals(PaymentOutbox.Status.PUBLISHED, row.getStatus());
    }

    @Test
    void failedRowsCanBeRetried() {
        PaymentOutbox row = PaymentOutbox.pending(
                UUID.randomUUID(), "PAYMENT_PENDING", "payment.events.v1", "payment-1", "{}");
        row.markProcessing();
        row.markFailed("Kafka unavailable", Instant.now().plusSeconds(2));

        assertEquals(PaymentOutbox.Status.FAILED, row.getStatus());
        row.requeue();
        assertEquals(PaymentOutbox.Status.PENDING, row.getStatus());
    }
}
