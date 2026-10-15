package com.example.lab.accounting.repository;

import com.example.lab.accounting.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    Optional<JournalEntry> findBySourceEventId(UUID sourceEventId);

    List<JournalEntry> findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(Instant from, Instant to);
}
