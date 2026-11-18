package com.example.lab.accounting.kafka;

import com.example.lab.accounting.service.AccountingService;
import com.example.lab.accounting.domain.kafka.PaymentEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentEventConsumer {
    private final AccountingService service;
    private final ObjectMapper mapper;

    @Observed(name = "accounting.kafka.consume")
    @org.springframework.kafka.annotation.RetryableTopic(attempts = "3", autoCreateTopics = "true", dltTopicSuffix = ".DLT")
    @KafkaListener(topics = "${payment.kafka.topic:payment.events.v1}", groupId = "${spring.kafka.consumer.group-id}", concurrency = "${lab.kafka.consumer-concurrency:3}")
    public void consume(String messEvent, Acknowledgment acknowledgment) {
        try {
            PaymentEvent event = mapper.readValue(messEvent, PaymentEvent.class);
            service.process(event);
        } catch (JsonProcessingException e) {
            log.error("error consume data", e);
        }
        acknowledgment.acknowledge();
    }
}
