package com.example.lab.payment.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "payment_outbox")
public class PaymentOutbox {
    public enum Status {PENDING, PROCESSING, PUBLISHED, FAILED}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @Column(name = "topic", nullable = false, length = 200)
    private String topic;
    @Column(name = "message_key", nullable = false, length = 200)
    private String messageKey;
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "available_at", nullable = false)
    private Instant availableAt;
    @Column(name = "locked_at")
    private Instant lockedAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "last_error", length = 2000)
    private String lastError;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static PaymentOutbox pending(UUID aggregateId, String eventType, String topic, String messageKey, String payload) {
        PaymentOutbox row = new PaymentOutbox();
        row.aggregateId = aggregateId;
        row.eventType = eventType;
        row.topic = topic;
        row.messageKey = messageKey;
        row.payload = payload;
        row.status = Status.PENDING;
        row.attempts = 0;
        row.availableAt = Instant.now();
        row.createdAt = row.availableAt;
        return row;
    }

    public void markProcessing() {
        status = Status.PROCESSING;
        lockedAt = Instant.now();
        attempts++;
    }

    public void markPublished() {
        status = Status.PUBLISHED;
        publishedAt = Instant.now();
        lockedAt = null;
        lastError = null;
    }

    public void markFailed(String error, Instant retryAt) {
        status = Status.FAILED;
        lockedAt = null;
        lastError = error == null ? null : error.substring(0, Math.min(error.length(), 2000));
        availableAt = retryAt;
    }

    public void requeue() {
        status = Status.PENDING;
        lockedAt = null;
        availableAt = Instant.now();
    }
}
