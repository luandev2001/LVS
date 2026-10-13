package com.example.lab.accounting.repository;
import com.example.lab.accounting.domain.ProcessedPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ProcessedPaymentEventRepository extends JpaRepository<ProcessedPaymentEvent, UUID> {}
