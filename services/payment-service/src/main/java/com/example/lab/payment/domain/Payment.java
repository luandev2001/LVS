package com.example.lab.payment.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_idempotency_key", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_payment_gateway_reference", columnNames = "gateway_reference")
})
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_reference", nullable = false, length = 100)
    private String orderReference;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "idempotency_key", nullable = false, length = 150)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(nullable = false)
    private int attempt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "callback_received_at")
    private Instant callbackReceivedAt;

    @Column(name = "saga_id")
    private UUID sagaId;
    @Column(name = "inventory_id")
    private Long inventoryId;
    @Column(name = "inventory_quantity")
    private Long inventoryQuantity;
    @Column(name = "saga_failure_mode", length = 50)
    private String sagaFailureMode;
    @Column(name = "saga_status", length = 40)
    private String sagaStatus;
    @Column(name = "saga_updated_at")
    private Instant sagaUpdatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static Payment create(String orderReference, BigDecimal amount, String currency, String idempotencyKey, String requestFingerprint) {
        Payment p = new Payment();
        p.orderReference = orderReference;
        p.amount = amount;
        p.currency = currency;
        p.idempotencyKey = idempotencyKey;
        p.requestFingerprint = requestFingerprint;
        p.status = PaymentStatus.CREATED;
        p.attempt = 0;
        p.createdAt = Instant.now();
        p.updatedAt = p.createdAt;
        return p;
    }

    public void startAttempt(String gatewayReference) {
        requireOneOf(PaymentStatus.CREATED, PaymentStatus.FAILED, PaymentStatus.TIMED_OUT);
        this.gatewayReference = gatewayReference;
        this.attempt++;
        this.status = PaymentStatus.PENDING;
        touch();
    }

    public CallbackResult applyCallback(boolean success) {
        if (status == PaymentStatus.SUCCEEDED) return CallbackResult.DUPLICATE;
        if (status == PaymentStatus.REFUNDED) return CallbackResult.IGNORED_REFUNDED;
        if (status != PaymentStatus.PENDING && status != PaymentStatus.TIMED_OUT) {
            return CallbackResult.IGNORED_INVALID_STATE;
        }
        this.status = success ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        this.callbackReceivedAt = Instant.now();
        touch();
        return CallbackResult.APPLIED;
    }

    public boolean timeoutIfExpired(Instant now, long timeoutSeconds) {
        if (status != PaymentStatus.PENDING) return false;
        if (updatedAt.plusSeconds(timeoutSeconds).isAfter(now)) return false;
        status = PaymentStatus.TIMED_OUT;
        touch();
        return true;
    }

    public void refund() {
        if (status != PaymentStatus.SUCCEEDED)
            throw new IllegalStateException("only succeeded payment can be refunded");
        status = PaymentStatus.REFUNDED;
        touch();
    }

    public void initializeSaga(UUID sagaId, Long inventoryId, long quantity, String failureMode) {
        this.sagaId = sagaId;
        this.inventoryId = inventoryId;
        this.inventoryQuantity = quantity;
        this.sagaFailureMode = failureMode;
        this.sagaStatus = "WAITING_ACCOUNTING";
        this.sagaUpdatedAt = Instant.now();
    }

    public void sagaAccountingPosted() {
        this.sagaStatus = "WAITING_INVENTORY";
        this.sagaUpdatedAt = Instant.now();
    }

    public void sagaInventoryReserved() {
        this.sagaStatus = "COMPLETED";
        this.sagaUpdatedAt = Instant.now();
    }

    public void sagaCompensating() {
        this.sagaStatus = "COMPENSATING";
        this.sagaUpdatedAt = Instant.now();
    }

    public void sagaCompensated() {
        this.sagaStatus = "COMPENSATED";
        this.sagaUpdatedAt = Instant.now();
    }

    public void sagaTimedOut() {
        this.sagaStatus = "TIMED_OUT";
        this.sagaUpdatedAt = Instant.now();
    }

    private void requireOneOf(PaymentStatus... allowed) {
        for (PaymentStatus candidate : allowed) if (status == candidate) return;
        throw new IllegalStateException("invalid payment state transition from " + status);
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    public enum CallbackResult {APPLIED, DUPLICATE, IGNORED_REFUNDED, IGNORED_INVALID_STATE}
}
