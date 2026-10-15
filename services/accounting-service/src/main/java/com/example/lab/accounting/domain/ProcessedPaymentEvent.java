package com.example.lab.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "processed_payment_events")
public class ProcessedPaymentEvent {
    @Id
    private UUID eventId;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private Instant processedAt = Instant.now();

    public ProcessedPaymentEvent(UUID eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
    }
}
