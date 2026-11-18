package com.example.lab.inventory.kafka;

import com.example.lab.inventory.domain.ProcessedSagaEvent;
import com.example.lab.inventory.repository.*;
import com.example.lab.inventory.service.InventoryService;
import com.example.lab.inventory.service.SagaOutboxService;
import com.example.lab.inventory.domain.kafka.PaymentEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import io.micrometer.observation.annotation.Observed;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class InventorySagaConsumer {
    private final ProcessedSagaEventRepository processed;
    private final InventoryService inventory;
    private final SagaOutboxService outbox;
    private final ObjectMapper mapper;

    @Observed(name = "inventory.saga.consume")
    @KafkaListener(topics = "${payment.kafka.topic:payment.events.v1}", groupId = "inventory-saga-consumer", concurrency = "${lab.kafka.consumer-concurrency:3}")
    @Transactional
    public void consume(String messEvent, Acknowledgment ack) {
        try {
            PaymentEvent e = mapper.readValue(messEvent, PaymentEvent.class);
            if (!"ACCOUNTING_POSTED".equals(e.eventType())) {
                ack.acknowledge();
                return;
            }
            if (processed.existsById(e.eventId())) {
                ack.acknowledge();
                return;
            }
            try {
                if ("INVENTORY_FAIL".equalsIgnoreCase(e.sagaFailureMode()))
                    throw new IllegalStateException("injected inventory saga failure");
                inventory.reserveAtomically(e.inventoryId(), e.quantity());
                outbox.enqueue(new PaymentEvent(UUID.randomUUID(), "INVENTORY_RESERVED", e.paymentId(), e.gatewayReference(), e.amount(), e.currency(), e.status(), e.attempt(), Instant.now(), e.sagaId(), e.inventoryId(), e.quantity(), e.sagaFailureMode()));
            } catch (Exception ex) {
                outbox.enqueue(new PaymentEvent(UUID.randomUUID(), "INVENTORY_RESERVATION_FAILED", e.paymentId(), e.gatewayReference(), e.amount(), e.currency(), e.status(), e.attempt(), Instant.now(), e.sagaId(), e.inventoryId(), e.quantity(), e.sagaFailureMode()));
            }
            processed.save(new ProcessedSagaEvent(e.eventId(), e.eventType()));
        } catch (JsonProcessingException e) {
            log.error("error consume data", e);
        }
        ack.acknowledge();
    }
}
