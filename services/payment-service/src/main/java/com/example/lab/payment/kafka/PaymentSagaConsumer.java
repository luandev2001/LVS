package com.example.lab.payment.kafka;

import com.example.lab.payment.domain.ProcessedSagaEvent;
import com.example.lab.payment.domain.kafka.PaymentEvent;
import com.example.lab.payment.repository.ProcessedSagaEventRepository;
import com.example.lab.payment.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import io.micrometer.observation.annotation.Observed;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentSagaConsumer {
    private final PaymentService service;
    private final ProcessedSagaEventRepository repository;
    private final ObjectMapper objectMapper;

    @Observed(name = "payment.saga.consume")
    @KafkaListener(topics = "${payment.kafka.topic:payment.events.v1}", groupId = "payment-saga-consumer", concurrency = "${lab.kafka.consumer-concurrency:3}")
    public void consume(String messEvent, Acknowledgment ack) {
        try {
            PaymentEvent event = objectMapper.readValue(messEvent, PaymentEvent.class);
            if (event.eventType().startsWith("ACCOUNTING_") || event.eventType().startsWith("INVENTORY_")) {
                if (repository.existsById(event.eventId())) {
                    ack.acknowledge();
                    return;
                }
                service.handleSagaEvent(event);
                repository.save(new ProcessedSagaEvent(event.eventId(), event.eventType()));
            }
        } catch (JsonProcessingException e) {
            log.error("error consume data", e);
        }

        ack.acknowledge();
    }
}
