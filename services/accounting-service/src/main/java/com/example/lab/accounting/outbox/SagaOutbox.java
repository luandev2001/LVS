package com.example.lab.accounting.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "saga_outbox")
public class SagaOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @Column(nullable = false)
    String eventType;
    @Column(nullable = false)
    String messageKey;
    @Column(nullable = false, columnDefinition = "text")
    String payload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Status status;
    @Column(nullable = false)
    int attempts;
    @Column(nullable = false)
    Instant availableAt;
    @Column(nullable = false)
    Instant createdAt;
    @Column(length = 2000)
    String lastError;

    public enum Status {PENDING, PROCESSING, PUBLISHED, FAILED}

    protected SagaOutbox() {
    }

    static SagaOutbox pending(String t, String k, String p) {
        SagaOutbox x = new SagaOutbox();
        x.eventType = t;
        x.messageKey = k;
        x.payload = p;
        x.status = Status.PENDING;
        x.availableAt = Instant.now();
        x.createdAt = x.availableAt;
        return x;
    }

    void processing() {
        status = Status.PROCESSING;
        attempts++;
    }

    void published() {
        status = Status.PUBLISHED;
    }

    void failed(String e) {
        status = Status.FAILED;
        lastError = e == null ? null : e.substring(0, Math.min(2000, e.length()));
        availableAt = Instant.now().plusSeconds(2);
    }

    void requeue() {
        status = Status.PENDING;
        availableAt = Instant.now();
    }
}
