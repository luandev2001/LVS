package com.example.lab.accounting.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class AccountingMetrics {
    private final Counter eventsReceived;
    private final Counter eventsDuplicate;
    private final Counter journalCreated;
    private final Counter journalRejected;
    private final Timer eventProcessing;

    public AccountingMetrics(MeterRegistry registry) {
        eventsReceived = registry.counter("accounting.kafka.events.received");
        eventsDuplicate = registry.counter("accounting.kafka.events.duplicate");
        journalCreated = registry.counter("accounting.journal.created");
        journalRejected = registry.counter("accounting.journal.rejected");
        eventProcessing = registry.timer("accounting.kafka.event.processing");
    }

    public void received() {
        eventsReceived.increment();
    }

    public void duplicate() {
        eventsDuplicate.increment();
    }

    public void journalCreated() {
        journalCreated.increment();
    }

    public void rejected() {
        journalRejected.increment();
    }

    public Timer.Sample start() {
        return Timer.start();
    }

    public void stop(Timer.Sample sample) {
        sample.stop(eventProcessing);
    }
}
