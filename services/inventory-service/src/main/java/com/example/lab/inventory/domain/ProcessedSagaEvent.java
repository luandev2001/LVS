package com.example.lab.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "processed_saga_events")
@NoArgsConstructor
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
