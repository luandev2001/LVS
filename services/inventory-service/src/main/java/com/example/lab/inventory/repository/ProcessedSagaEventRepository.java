package com.example.lab.inventory.repository;

import com.example.lab.inventory.domain.ProcessedSagaEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedSagaEventRepository extends JpaRepository<ProcessedSagaEvent, UUID> {
}
