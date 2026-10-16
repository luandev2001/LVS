package com.example.lab.payment.outbox;

import com.example.lab.payment.domain.PaymentOutbox;
import com.example.lab.payment.repository.PaymentOutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentOutboxMetrics {
    public PaymentOutboxMetrics(PaymentOutboxRepository repository, MeterRegistry registry) {
        Gauge.builder("payment.outbox.pending", repository, r -> r.countByStatus(PaymentOutbox.Status.PENDING))
                .description("Pending outbox rows").register(registry);
        Gauge.builder("payment.outbox.processing", repository, r -> r.countByStatus(PaymentOutbox.Status.PROCESSING))
                .description("Processing outbox rows").register(registry);
        Gauge.builder("payment.outbox.failed", repository, r -> r.countByStatus(PaymentOutbox.Status.FAILED))
                .description("Failed outbox rows").register(registry);
        Gauge.builder("payment.outbox.published", repository, r -> r.countByStatus(PaymentOutbox.Status.PUBLISHED))
                .description("Published outbox rows").register(registry);
    }
}
