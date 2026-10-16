package com.example.lab.accounting.service;

import com.example.lab.accounting.domain.Account;
import com.example.lab.accounting.domain.JournalEntry;
import com.example.lab.accounting.outbox.SagaOutboxService;
import com.example.lab.accounting.domain.kafka.PaymentEvent;
import com.example.lab.accounting.observability.AccountingMetrics;
import com.example.lab.accounting.repository.AccountRepository;
import com.example.lab.accounting.repository.JournalEntryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountingServiceTest {
    @Test
    void successfulPaymentCreatesBalancedJournal() {
        AccountRepository accounts = mock(AccountRepository.class);
        JournalEntryRepository journals = mock(JournalEntryRepository.class);
        var processed = mock(com.example.lab.accounting.repository.ProcessedPaymentEventRepository.class);
        AccountingMetrics metrics = mock(AccountingMetrics.class);
        Account cash = account("CASH");
        Account revenue = account("PAYMENT_REVENUE");
        when(accounts.findByCode("CASH")).thenReturn(Optional.of(cash));
        when(accounts.findByCode("PAYMENT_REVENUE")).thenReturn(Optional.of(revenue));
        when(journals.findBySourceEventId(any())).thenReturn(Optional.empty());

        SagaOutboxService outboxService = mock(SagaOutboxService.class);
        AccountingService service = new AccountingService(accounts, journals, processed, metrics, outboxService);
        PaymentEvent event = event("PAYMENT_SUCCEEDED");
        service.process(event);

        verify(journals).save(any(JournalEntry.class));
        verify(metrics).journalCreated();
    }

    @Test
    void duplicateEventDoesNotCreateSecondJournal() {
        AccountRepository accounts = mock(AccountRepository.class);
        JournalEntryRepository journals = mock(JournalEntryRepository.class);
        var processed = mock(com.example.lab.accounting.repository.ProcessedPaymentEventRepository.class);
        AccountingMetrics metrics = mock(AccountingMetrics.class);
        when(processed.findById(any())).thenReturn(Optional.of(Mockito.mock(com.example.lab.accounting.domain.ProcessedPaymentEvent.class)));
        SagaOutboxService outboxService = mock(SagaOutboxService.class);
        new AccountingService(accounts, journals, processed, metrics, outboxService).process(event("PAYMENT_SUCCEEDED"));

        verify(journals, never()).save(any());
        verify(metrics).duplicate();
    }

    private static PaymentEvent event(String type) {
        return new PaymentEvent(UUID.randomUUID(), type, UUID.randomUUID(), "gw-1", new BigDecimal("100.00"), "USD", "SUCCEEDED", 1, Instant.now());
    }

    private static Account account(String code) {
        try {
            var c = Account.class.getDeclaredConstructor();
            c.setAccessible(true);
            Account a = c.newInstance();
            var f = Account.class.getDeclaredField("code");
            f.setAccessible(true);
            f.set(a, code);
            return a;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
