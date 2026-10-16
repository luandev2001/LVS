package com.example.lab.payment.repository;

import com.example.lab.payment.domain.ProcessedSagaEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedSagaEventRepository extends JpaRepository<ProcessedSagaEvent, UUID> {
}
