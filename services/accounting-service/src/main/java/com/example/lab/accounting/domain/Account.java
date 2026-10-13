package com.example.lab.accounting.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "account")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    protected Account() {}
    public Long getId(){return id;} public String getCode(){return code;} public String getName(){return name;}
    public String getCurrency(){return currency;} public boolean isActive(){return active;}
}
