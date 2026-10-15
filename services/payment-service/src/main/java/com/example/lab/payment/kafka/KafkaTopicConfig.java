package com.example.lab.payment.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {
    @Bean
    NewTopic paymentEventsTopic(@Value("${payment.kafka.topic:payment.events.v1}") String topic) {
        return new NewTopic(topic, 3, (short) 1);
    }

    @Bean
    NewTopic paymentEventsDlqTopic(@Value("${payment.kafka.dlq-topic:payment.events.dlq.v1}") String topic) {
        return new NewTopic(topic, 3, (short) 1);
    }
}
