package com.example.lab.payment.outbox;

import com.example.lab.payment.domain.PaymentOutbox;
import com.example.lab.payment.domain.kafka.PaymentEvent;
import com.example.lab.payment.repository.PaymentOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Component
public class PaymentOutboxPublisher {
    private final PaymentOutboxRepository repository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${payment.outbox.batch-size:50}")
    private int batchSize;
    @Value("${payment.outbox.publisher-concurrency:2}")
    private int concurrency;
    @Value("${payment.outbox.stale-processing-ms:10000}")
    private long staleProcessingMs;
    @Value("${payment.outbox.simulate-crash-after-send:false}")
    private boolean simulateCrashAfterSend;
    private final AtomicInteger runningWorkers = new AtomicInteger();

    @Scheduled(fixedDelayString = "${payment.outbox.polling-ms:500}")
    public void poll() {
        transactionTemplate.executeWithoutResult(status ->
                repository.requeueStaleProcessing(Instant.now().minusMillis(staleProcessingMs)));

        while (runningWorkers.get() < concurrency && runningWorkers.compareAndSet(
                runningWorkers.get(), runningWorkers.get() + 1)) {
            Thread.startVirtualThread(this::publishOneBatch);
        }
    }

    private void publishOneBatch() {
        try {
            List<PaymentOutbox> batch = transactionTemplate.execute(status -> {
                List<PaymentOutbox> rows = repository.lockPendingBatch(batchSize);
                rows.forEach(PaymentOutbox::markProcessing);
                return rows;
            });
            if (batch == null || batch.isEmpty()) return;

            for (PaymentOutbox row : batch) publish(row);
        } finally {
            runningWorkers.decrementAndGet();
        }
    }

    private void publish(PaymentOutbox row) {
        try {
            PaymentEvent event = objectMapper.readValue(row.getPayload(), PaymentEvent.class);
            kafkaTemplate.send(row.getTopic(), row.getMessageKey(), event).get();
            if (simulateCrashAfterSend) {
                throw new SimulatedPublisherCrash("simulated crash after Kafka send and before outbox status update");
            }
            transactionTemplate.executeWithoutResult(status -> {
                PaymentOutbox current = repository.findById(row.getId()).orElseThrow();
                current.markPublished();
                repository.save(current);
            });
        } catch (Exception ex) {
            if (ex instanceof SimulatedPublisherCrash) return;
            transactionTemplate.executeWithoutResult(status -> {
                PaymentOutbox current = repository.findById(row.getId()).orElseThrow();
                current.markFailed(ex.getMessage(), Instant.now().plusSeconds(2));
                repository.save(current);
            });
        }
    }

    public int recoverStaleNow() {
        return transactionTemplate.execute(status -> repository.requeueStaleProcessing(
                Instant.now().minusMillis(staleProcessingMs)));
    }

    public long count(PaymentOutbox.Status status) {
        return repository.countByStatus(status);
    }

    private static class SimulatedPublisherCrash extends RuntimeException {
        SimulatedPublisherCrash(String message) {
            super(message);
        }
    }
}
