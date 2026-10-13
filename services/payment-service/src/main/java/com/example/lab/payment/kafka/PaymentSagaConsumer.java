package com.example.lab.payment.kafka;

import com.example.lab.payment.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener; import io.micrometer.observation.annotation.Observed;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class PaymentSagaConsumer {
    private final PaymentService service;
    private final ProcessedSagaEventRepository repository;
    public PaymentSagaConsumer(PaymentService service, ProcessedSagaEventRepository repository){this.service=service;this.repository=repository;}
    @Observed(name="payment.saga.consume")
    @KafkaListener(topics="${payment.kafka.topic:payment.events.v1}", groupId="payment-saga-consumer", concurrency="${lab.kafka.consumer-concurrency:3}")
    public void consume(PaymentEvent event, Acknowledgment ack){
        if (event.eventType().startsWith("ACCOUNTING_") || event.eventType().startsWith("INVENTORY_")) {
            if (repository.existsById(event.eventId())) { ack.acknowledge(); return; }
            service.handleSagaEvent(event);
            repository.save(new ProcessedSagaEvent(event.eventId(), event.eventType()));
        }
        ack.acknowledge();
    }
}
