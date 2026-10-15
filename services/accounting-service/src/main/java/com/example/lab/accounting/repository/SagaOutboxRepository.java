package com.example.lab.accounting.repository;

import com.example.lab.accounting.outbox.SagaOutbox;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface SagaOutboxRepository extends JpaRepository<SagaOutbox, UUID> {
    @Query(value = "select * from saga_outbox where status in ('PENDING','FAILED') and available_at <= current_timestamp order by created_at asc limit :limit for update skip locked", nativeQuery = true)
    List<SagaOutbox> lockBatch(@Param("limit") int limit);

    @Modifying
    @Query("update SagaOutbox x set x.status='PENDING', x.availableAt=current_timestamp where x.status='PROCESSING' and x.availableAt < :cutoff")
    int requeueStale(@Param("cutoff") Instant cutoff);

    @Query("select count(x) from SagaOutbox x where x.status = :status")
    long count(@Param("status") SagaOutbox.Status status);
}
