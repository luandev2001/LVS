package com.example.lab.accounting.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="journal_line")
public class JournalLine {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="journal_entry_id", nullable=false)
    private JournalEntry journalEntry;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="account_id", nullable=false)
    private Account account;
    @Column(nullable=false, length=10)
    private String side;
    @Column(nullable=false, precision=19, scale=4)
    private BigDecimal amount;
    @Column(nullable=false, length=3)
    private String currency;
    protected JournalLine() {}
    JournalLine(JournalEntry entry, Account account, String side, BigDecimal amount, String currency) {
        this.journalEntry=entry; this.account=account; this.side=side; this.amount=amount; this.currency=currency;
    }
    public Long getId(){return id;} public Account getAccount(){return account;} public String getSide(){return side;}
    public BigDecimal getAmount(){return amount;} public String getCurrency(){return currency;}
}
