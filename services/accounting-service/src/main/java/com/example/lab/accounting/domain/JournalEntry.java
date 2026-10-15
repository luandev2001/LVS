package com.example.lab.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "journal_entry")
public class JournalEntry {
    @Id
    private UUID id;
    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    @Column(name = "entry_type", nullable = false)
    private String entryType;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalLine> lines = new ArrayList<>();

    public JournalEntry(UUID sourceEventId, UUID paymentId, String entryType, String description, String currency, Instant occurredAt) {
        this.id = UUID.randomUUID();
        this.sourceEventId = sourceEventId;
        this.paymentId = paymentId;
        this.entryType = entryType;
        this.description = description;
        this.currency = currency;
        this.occurredAt = occurredAt;
    }

    public void addLine(Account account, String side, java.math.BigDecimal amount) {
        lines.add(new JournalLine(this, account, side, amount, currency));
    }
}
