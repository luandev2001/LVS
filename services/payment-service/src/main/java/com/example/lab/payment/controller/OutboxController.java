package com.example.lab.payment.controller;

import com.example.lab.payment.domain.PaymentOutbox;
import com.example.lab.payment.outbox.PaymentOutboxPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/outbox")
public class OutboxController {
    private final PaymentOutboxPublisher publisher;

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return Map.of(
                "pending", publisher.count(PaymentOutbox.Status.PENDING),
                "processing", publisher.count(PaymentOutbox.Status.PROCESSING),
                "published", publisher.count(PaymentOutbox.Status.PUBLISHED),
                "failed", publisher.count(PaymentOutbox.Status.FAILED));
    }

    @PostMapping("/recover-stale")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> recoverStale() {
        int recovered = publisher.recoverStaleNow();
        return Map.of("recovered", recovered, "at", Instant.now().toString());
    }
}
