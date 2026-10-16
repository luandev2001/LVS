package com.example.lab.accounting.service;

import com.example.lab.accounting.domain.*;
import com.example.lab.accounting.observability.AccountingMetrics;
import com.example.lab.accounting.repository.*;
import com.example.lab.accounting.outbox.SagaOutboxService;
import com.example.lab.accounting.domain.kafka.PaymentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.time.YearMonth;

@Service
public class AccountingService {
    private final AccountRepository accounts;
    private final JournalEntryRepository journals;
    private final ProcessedPaymentEventRepository processedEvents;
    private final AccountingMetrics metrics;
    private final SagaOutboxService sagaOutbox;

    public AccountingService(AccountRepository accounts, JournalEntryRepository journals, ProcessedPaymentEventRepository processedEvents, AccountingMetrics metrics, SagaOutboxService sagaOutbox) {
        this.accounts = accounts;
        this.journals = journals;
        this.processedEvents = processedEvents;
        this.metrics = metrics;
        this.sagaOutbox = sagaOutbox;
    }

    @Transactional
    public void process(PaymentEvent event) {
        metrics.received();
        if (processedEvents.findById(event.eventId()).isPresent()) {
            metrics.duplicate();
            return;
        }
        if ("PAYMENT_SUCCEEDED".equals(event.eventType())) {
            if ("ACCOUNTING_FAIL".equalsIgnoreCase(event.sagaFailureMode()))
                throw new IllegalStateException("injected accounting saga failure");
            createBalancedEntry(event, "PAYMENT_SETTLEMENT", "Payment settlement");
            sagaOutbox.enqueue(new PaymentEvent(UUID.randomUUID(), "ACCOUNTING_POSTED", event.paymentId(), event.gatewayReference(), event.amount(), event.currency(), event.status(), event.attempt(), Instant.now(), event.sagaId(), event.inventoryId(), event.quantity(), event.sagaFailureMode()));
        } else if ("INVENTORY_RESERVATION_FAILED".equals(event.eventType())) {
//            createReversalEntry(event);
            sagaOutbox.enqueue(new PaymentEvent(UUID.randomUUID(), "ACCOUNTING_REVERSED", event.paymentId(), event.gatewayReference(), event.amount(), event.currency(), event.status(), event.attempt(), Instant.now(), event.sagaId(), event.inventoryId(), event.quantity(), event.sagaFailureMode()));
        } else if ("PAYMENT_REFUNDED".equals(event.eventType())) {
            createBalancedEntry(event, "PAYMENT_REFUND", "Payment refund");
        }
        processedEvents.save(new com.example.lab.accounting.domain.ProcessedPaymentEvent(event.eventId(), event.eventType()));
    }

    private void createBalancedEntry(PaymentEvent event, String type, String description) {
        try {
            String currency = event.currency().toUpperCase(Locale.ROOT);
            Account cash = accounts.findByCode("CASH").orElseThrow();
            Account offset = accounts.findByCode("PAYMENT_REFUND".equals(type) ? "REFUND_EXPENSE" : "PAYMENT_REVENUE").orElseThrow();
            JournalEntry entry = new JournalEntry(event.eventId(), event.paymentId(), type, description, currency,
                    Optional.ofNullable(event.occurredAt()).orElse(Instant.now()));
            BigDecimal amount = event.amount();
            if (amount == null || amount.signum() <= 0)
                throw new IllegalArgumentException("event amount must be positive");
            entry.addLine(cash, "PAYMENT_REFUND".equals(type) ? "CREDIT" : "DEBIT", amount);
            entry.addLine(offset, "PAYMENT_REFUND".equals(type) ? "DEBIT" : "CREDIT", amount);
            validateBalanced(entry);
            journals.save(entry);
            metrics.journalCreated();
        } catch (RuntimeException ex) {
            metrics.rejected();
            throw ex;
        }
    }

    private void validateBalanced(JournalEntry entry) {
        BigDecimal debit = entry.getLines().stream().filter(l -> "DEBIT".equals(l.getSide())).map(JournalLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = entry.getLines().stream().filter(l -> "CREDIT".equals(l.getSide())).map(JournalLine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (debit.compareTo(credit) != 0) throw new IllegalStateException("journal entry is not balanced");
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> list(int limit) {
        return journals.findAll(org.springframework.data.domain.PageRequest.of(0, Math.min(Math.max(limit, 1), 200),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "occurredAt"))).getContent();
    }

    @Transactional(readOnly = true)
    public MonthlySummary monthlySummary(YearMonth month) {
        Instant from = month.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        List<JournalEntry> entries = journals.findByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(from, to);
        BigDecimal debit = entries.stream().flatMap(e -> e.getLines().stream()).filter(l -> "DEBIT".equals(l.getSide()))
                .map(JournalLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = entries.stream().flatMap(e -> e.getLines().stream()).filter(l -> "CREDIT".equals(l.getSide()))
                .map(JournalLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MonthlySummary(month.toString(), entries.size(), debit, credit, debit.compareTo(credit) == 0);
    }

    public record MonthlySummary(String month, int journalEntries, BigDecimal totalDebit, BigDecimal totalCredit,
                                 boolean balanced) {
    }
}
