package com.example.lab.accounting.controller;

import com.example.lab.accounting.domain.JournalEntry;
import com.example.lab.accounting.domain.JournalLine;
import com.example.lab.accounting.service.AccountingService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/accounting")
public class AccountingController {
    private final AccountingService service;
    public AccountingController(AccountingService service){this.service=service;}

    @GetMapping("/journal-entries")
    public List<JournalEntryResponse> journalEntries(@RequestParam(defaultValue="50") int limit){
        return service.list(limit).stream().map(JournalEntryResponse::from).toList();
    }

    @GetMapping("/monthly-summary")
    public AccountingService.MonthlySummary monthlySummary(@RequestParam String month) {
        return service.monthlySummary(YearMonth.parse(month));
    }

    public record JournalEntryResponse(java.util.UUID id, java.util.UUID sourceEventId, java.util.UUID paymentId,
                                       String entryType, String currency, java.time.Instant occurredAt,
                                       List<Line> lines) {
        static JournalEntryResponse from(JournalEntry e) {
            return new JournalEntryResponse(e.getId(), e.getSourceEventId(), e.getPaymentId(), e.getEntryType(),
                    e.getCurrency(), e.getOccurredAt(), e.getLines().stream().map(l -> new Line(l.getAccount().getCode(), l.getSide(), l.getAmount(), l.getCurrency())).toList());
        }
        public record Line(String account, String side, BigDecimal amount, String currency) {}
    }
}
