package com.example.lab.payment.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, UUID> {
    @Modifying
    @Query(value = """
        update payment_outbox
           set status = 'PENDING', locked_at = null, available_at = current_timestamp
         where status = 'PROCESSING' and locked_at < :cutoff
        """, nativeQuery = true)
    int requeueStaleProcessing(@Param("cutoff") Instant cutoff);

    @Query(value = """
        select * from payment_outbox
         where status in ('PENDING', 'FAILED')
           and available_at <= current_timestamp
         order by created_at asc
         limit :limit
         for update skip locked
        """, nativeQuery = true)
    List<PaymentOutbox> lockPendingBatch(@Param("limit") int limit);

    long countByStatus(PaymentOutbox.Status status);
}
