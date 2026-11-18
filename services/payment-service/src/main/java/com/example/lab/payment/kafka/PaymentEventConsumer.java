package com.example.lab.payment.kafka;

import com.example.lab.payment.domain.ProcessedPaymentEvent;
import com.example.lab.payment.domain.kafka.PaymentEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentEventConsumer {
    private final ProcessedPaymentEventRepository repository;
    private final ObjectMapper objectMapper;

    @RetryableTopic(attempts = "3", autoCreateTopics = "true", dltTopicSuffix = ".DLT")
    @KafkaListener(topics = "${payment.kafka.topic:payment.events.v1}", groupId = "${spring.kafka.consumer.group-id}", concurrency = "${lab.kafka.consumer-concurrency:3}")
    @Transactional
    public void consume(String messEvent, Acknowledgment acknowledgment) {
        try {
            PaymentEvent event = objectMapper.readValue(messEvent, PaymentEvent.class);

            if (repository.existsById(event.eventId())) {
                acknowledgment.acknowledge();
                return;
            }
            if ("POISON_MESSAGE".equals(event.eventType())) {
                throw new IllegalStateException("deliberate Phase 6 poison message");
            }
            repository.saveAndFlush(new ProcessedPaymentEvent(event.eventId(), event.eventType()));
        } catch (JsonProcessingException | DataIntegrityViolationException e) {
            // Another delivery won the insert race. Treat it as an already-processed event.
            log.error("error consume data", e);
        }

        acknowledgment.acknowledge();
    }
}
