package com.example.lab.payment.outbox;

import com.example.lab.payment.domain.Payment;
import com.example.lab.payment.kafka.PaymentEvent;
import com.example.lab.payment.kafka.PaymentEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentOutboxService {
    private final PaymentOutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final PaymentEventPublisher eventPublisher;
    private final String topic;

    public PaymentOutboxService(PaymentOutboxRepository repository,
                                ObjectMapper objectMapper,
                                PaymentEventPublisher eventPublisher,
                                @Value("${payment.kafka.topic:payment.events.v1}") String topic) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.topic = topic;
    }

    @Transactional
    public void enqueue(Payment payment, String eventType) {
        save(eventPublisher.createEvent(payment, eventType));
    }

    @Transactional
    public void enqueueDuplicate(Payment payment) {
        PaymentEvent event = eventPublisher.createEvent(payment, "DUPLICATE_OUTBOX_INJECTED");
        save(event);
        save(event);
    }

    private void save(PaymentEvent event) {
        try {
            repository.save(PaymentOutbox.pending(event.paymentId(), event.eventType(), topic,
                    event.paymentId().toString(), objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unable to serialize payment event", e);
        }
    }

    public long count(PaymentOutbox.Status status) { return repository.countByStatus(status); }

    @Transactional
    public int recoverStale(Instant cutoff) { return repository.requeueStaleProcessing(cutoff); }
}
