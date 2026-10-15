package com.example.lab.payment.service;

import com.example.lab.payment.domain.GatewayMode;
import com.example.lab.payment.domain.Payment;
import com.example.lab.payment.domain.PaymentStatus;
import com.example.lab.payment.gateway.SimulatedPaymentGateway;
import com.example.lab.payment.repository.PaymentRepository;
import com.example.lab.payment.outbox.PaymentOutboxService;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository repository;
    private final SimulatedPaymentGateway gateway;
    private final long timeoutSeconds;
    private final TransactionTemplate transactionTemplate;
    private final PaymentOutboxService outboxService;

    public PaymentService(PaymentRepository repository, SimulatedPaymentGateway gateway,
                          PlatformTransactionManager transactionManager,
                          PaymentOutboxService outboxService,
                          @Value("${payment.timeout-seconds:2}") long timeoutSeconds) {
        this.repository = repository;
        this.gateway = gateway;
        this.timeoutSeconds = timeoutSeconds;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.outboxService = outboxService;
    }

    public Payment create(String orderReference, BigDecimal amount, String currency,
                          String idempotencyKey, GatewayMode mode, Long inventoryId, long quantity, String sagaFailureMode) {
        validateCreateInput(amount, currency, idempotencyKey);
        String normalizedCurrency = currency.toUpperCase();
        String fingerprint = fingerprint(orderReference, amount, normalizedCurrency, inventoryId, quantity, sagaFailureMode);

        Payment existing = repository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) return returnIfSameRequest(existing, fingerprint);

        try {
            return transactionTemplate.execute(status -> createTransactionally(
                    orderReference, amount, normalizedCurrency, idempotencyKey, fingerprint, mode, inventoryId, quantity, sagaFailureMode));
        } catch (DataIntegrityViolationException race) {
            // Two requests with the same key can race. The database unique constraint is the final arbiter.
            Payment winner = repository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> race);
            return returnIfSameRequest(winner, fingerprint);
        }
    }

    private Payment createTransactionally(String orderReference, BigDecimal amount, String currency,
                                          String idempotencyKey, String fingerprint, GatewayMode mode,
                                          Long inventoryId, long quantity, String sagaFailureMode) {
        Payment payment = repository.save(Payment.create(orderReference, amount, currency, idempotencyKey, fingerprint));
        String reference = gateway.createGatewayTransaction();
        payment.startAttempt(reference);
        Payment saved = repository.save(payment);
        if (inventoryId != null) {
            if (quantity <= 0) throw new IllegalArgumentException("inventory quantity must be positive");
            saved.initializeSaga(UUID.randomUUID(), inventoryId, quantity, sagaFailureMode == null ? "NONE" : sagaFailureMode);
            saved = repository.save(saved);
        }
        outboxService.enqueue(saved, "PAYMENT_PENDING");
        dispatchAfterCommit(saved.getId(), reference, saved.getAmount(), saved.getCurrency(), mode);
        return saved;
    }

    private Payment returnIfSameRequest(Payment existing, String fingerprint) {
        if (!existing.getRequestFingerprint().equals(fingerprint)) {
            throw new IllegalStateException("idempotency key was already used with different payment parameters");
        }
        return existing;
    }

    private void validateCreateInput(BigDecimal amount, String currency, String idempotencyKey) {
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
        if (currency == null || currency.length() != 3)
            throw new IllegalArgumentException("currency must be ISO-4217 style 3 letters");
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("Idempotency-Key must not be blank");
    }

    private String fingerprint(String orderReference, BigDecimal amount, String currency, Long inventoryId, long quantity, String sagaFailureMode) {
        String payload = String.valueOf(orderReference) + ":" + amount.setScale(2).toPlainString() + ":" + currency + ":" + inventoryId + ":" + quantity + ":" + String.valueOf(sagaFailureMode).toUpperCase();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("unable to fingerprint request", e);
        }
    }

    @Transactional
    public Payment retry(UUID id, GatewayMode mode) {
        Payment payment = get(id);
        String reference = gateway.createGatewayTransaction();
        payment.startAttempt(reference);
        Payment saved = repository.save(payment);
        outboxService.enqueue(saved, "PAYMENT_PENDING");
        dispatchAfterCommit(saved.getId(), reference, saved.getAmount(), saved.getCurrency(), mode);
        return saved;
    }


    private void dispatchAfterCommit(UUID paymentId, String reference, BigDecimal amount, String currency, GatewayMode mode) {
        Runnable dispatch = () -> gateway.dispatch(paymentId, reference, amount, currency, mode, callback ->
                receiveWebhook(callback.paymentId(), callback.gatewayReference(), callback.amount(), callback.currency(), callback.success(), callback.signature()));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }

    public CallbackResult receiveWebhook(UUID paymentId, String gatewayReference, BigDecimal amount, String currency, boolean success, String signature) {
        return transactionTemplate.execute(status -> receiveWebhookInTransaction(paymentId, gatewayReference, amount, currency, success, signature));
    }

    private CallbackResult receiveWebhookInTransaction(UUID paymentId, String gatewayReference, BigDecimal amount, String currency, boolean success, String signature) {
        Payment payment = repository.findByIdForUpdate(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (!gateway.verify(paymentId, gatewayReference, amount, currency, success, signature)) {
            return new CallbackResult(payment.getStatus().name(), "INVALID_SIGNATURE", false);
        }
        if (!gatewayReference.equals(payment.getGatewayReference())) {
            return new CallbackResult(payment.getStatus().name(), "REFERENCE_MISMATCH", false);
        }
        if (payment.getAmount().compareTo(amount) != 0 || !payment.getCurrency().equalsIgnoreCase(currency)) {
            return new CallbackResult(payment.getStatus().name(), "AMOUNT_OR_CURRENCY_MISMATCH", false);
        }
        Payment.CallbackResult result = payment.applyCallback(success);
        repository.save(payment);
        if (result == Payment.CallbackResult.APPLIED) {
            outboxService.enqueue(payment, "PAYMENT_" + payment.getStatus().name());
        }
        return new CallbackResult(payment.getStatus().name(), result.name(), result == Payment.CallbackResult.APPLIED);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${payment.timeout-scan-ms:500}")
    public void markExpiredPayments() {
        Instant cutoff = Instant.now().minusSeconds(timeoutSeconds);
        repository.findByStatusAndUpdatedAtBefore(com.example.lab.payment.domain.PaymentStatus.PENDING, cutoff)
                .forEach(p -> {
                    if (p.timeoutIfExpired(Instant.now(), timeoutSeconds)) {
                        repository.save(p);
                        outboxService.enqueue(p, "PAYMENT_TIMED_OUT");
                    }
                });
    }

    @Transactional
    public Payment refund(UUID id) {
        Payment payment = get(id);
        payment.refund();
        Payment saved = repository.save(payment);
        outboxService.enqueue(saved, "PAYMENT_REFUNDED");
        return saved;
    }

    @Transactional
    public void handleSagaEvent(com.example.lab.payment.kafka.PaymentEvent event) {
        Payment payment = repository.findByIdForUpdate(event.paymentId()).orElseThrow(() -> new PaymentNotFoundException(event.paymentId()));
        if (payment.getSagaId() == null || event.sagaId() == null || !payment.getSagaId().equals(event.sagaId()))
            return;
        switch (event.eventType()) {
            case "ACCOUNTING_POSTED" -> {
                payment.sagaAccountingPosted();
                repository.save(payment);
            }
            case "INVENTORY_RESERVED" -> {
                payment.sagaInventoryReserved();
                repository.save(payment);
            }
            case "INVENTORY_RESERVATION_FAILED" -> {
                if (payment.getStatus() == PaymentStatus.SUCCEEDED) payment.refund();
                payment.sagaCompensating();
                repository.save(payment);
            }
            case "ACCOUNTING_REVERSED" -> {
                payment.sagaCompensated();
                repository.save(payment);
                outboxService.enqueue(payment, "PAYMENT_COMPENSATED");
            }
            default -> {
            }
        }
    }

    @Transactional
    @Scheduled(fixedDelayString = "${payment.saga-timeout-scan-ms:1000}")
    public void markSagaTimeouts() {
        Instant cutoff = Instant.now().minusSeconds(10);
        repository.findBySagaStatusInAndSagaUpdatedAtBefore(java.util.List.of("WAITING_ACCOUNTING", "WAITING_INVENTORY"), cutoff)
                .forEach(p -> {
                    p.sagaTimedOut();
                    repository.save(p);
                    outboxService.enqueue(p, "PAYMENT_SAGA_TIMED_OUT");
                });
    }

    public Payment get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }

    @Transactional
    public void publishDuplicateEvent(UUID id) {
        outboxService.enqueueDuplicate(get(id));
    }

    public record CallbackResult(String status, String outcome, boolean applied) {
    }

    public static class PaymentNotFoundException extends RuntimeException {
        public PaymentNotFoundException(UUID id) {
            super("payment not found: " + id);
        }
    }
}
