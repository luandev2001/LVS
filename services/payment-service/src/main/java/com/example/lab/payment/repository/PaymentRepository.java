package com.example.lab.payment.repository;

import com.example.lab.payment.domain.Payment;
import com.example.lab.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByIdForUpdate(UUID id);

    long countByStatus(PaymentStatus status);

    List<Payment> findByStatusAndUpdatedAtBefore(PaymentStatus status, Instant cutoff);

    List<com.example.lab.payment.domain.Payment> findBySagaStatusInAndSagaUpdatedAtBefore(java.util.Collection<String> statuses, Instant cutoff);
}

