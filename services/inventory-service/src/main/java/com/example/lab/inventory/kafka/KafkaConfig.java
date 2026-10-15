package com.example.lab.inventory.kafka;

import com.example.lab.payment.kafka.PaymentEvent;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.*;

import java.util.*;

@Configuration
public class KafkaConfig {
    @Bean
    ConsumerFactory<String, PaymentEvent> inventoryConsumerFactory(@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String b) {
        Map<String, Object> p = new HashMap<>();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, b);
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JsonDeserializer<PaymentEvent> d = new JsonDeserializer<>(PaymentEvent.class);
        d.addTrustedPackages("com.example.lab.payment.kafka");
        d.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(p, new StringDeserializer(), d);
    }

    @Bean
    ProducerFactory<String, PaymentEvent> inventoryProducerFactory(@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String b) {
        Map<String, Object> p = new HashMap<>();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, b);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(p);
    }

    @Bean
    KafkaTemplate<String, PaymentEvent> inventoryKafkaTemplate(ProducerFactory<String, PaymentEvent> f) {
        return new KafkaTemplate<>(f);
    }
}
