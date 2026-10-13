package com.example.lab.accounting.kafka;

import com.example.lab.accounting.service.AccountingService;
import io.micrometer.observation.annotation.Observed;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {
    private final AccountingService service;
    public PaymentEventConsumer(AccountingService service){this.service=service;}

    @Observed(name="accounting.kafka.consume")
    @org.springframework.kafka.annotation.RetryableTopic(attempts = "3", autoCreateTopics = "true", dltTopicSuffix = ".DLT")
    @KafkaListener(topics="${payment.kafka.topic:payment.events.v1}", groupId="${spring.kafka.consumer.group-id}", concurrency="${lab.kafka.consumer-concurrency:3}")
    public void consume(PaymentEvent event, Acknowledgment acknowledgment) {
        service.process(event);
        acknowledgment.acknowledge();
    }
}
