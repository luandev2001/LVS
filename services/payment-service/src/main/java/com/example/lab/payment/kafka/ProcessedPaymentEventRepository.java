package com.example.lab.payment.kafka;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProcessedPaymentEventRepository extends JpaRepository<ProcessedPaymentEvent, UUID> {}
