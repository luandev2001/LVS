package com.example.lab.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "processed_saga_events")
public class ProcessedSagaEvent {
    @Id
    UUID eventId;
    @Column(nullable = false)
    String eventType;
    @Column(nullable = false)
    Instant processedAt = Instant.now();

    public ProcessedSagaEvent(UUID id, String type) {
        eventId = id;
        eventType = type;
    }
}
