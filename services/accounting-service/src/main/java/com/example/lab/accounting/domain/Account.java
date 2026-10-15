package com.example.lab.accounting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
}
