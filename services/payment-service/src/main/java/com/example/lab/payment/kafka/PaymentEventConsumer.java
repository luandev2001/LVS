package com.example.lab.payment.kafka;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventConsumer {
    private final ProcessedPaymentEventRepository repository;

    public PaymentEventConsumer(ProcessedPaymentEventRepository repository) {
        this.repository = repository;
    }

    @RetryableTopic(attempts = "3", autoCreateTopics = "true", dltTopicSuffix = ".DLT")
    @KafkaListener(topics = "${payment.kafka.topic:payment.events.v1}", groupId = "${spring.kafka.consumer.group-id}", concurrency = "${lab.kafka.consumer-concurrency:3}")
    @Transactional
    public void consume(PaymentEvent event, Acknowledgment acknowledgment) {
        if (repository.existsById(event.eventId())) {
            acknowledgment.acknowledge();
            return;
        }
        if ("POISON_MESSAGE".equals(event.eventType())) {
            throw new IllegalStateException("deliberate Phase 6 poison message");
        }
        try {
            repository.saveAndFlush(new ProcessedPaymentEvent(event.eventId(), event.eventType()));
        } catch (DataIntegrityViolationException duplicate) {
            // Another delivery won the insert race. Treat it as an already-processed event.
        }
        acknowledgment.acknowledge();
    }
}
