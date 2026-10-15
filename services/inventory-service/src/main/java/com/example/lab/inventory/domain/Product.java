package com.example.lab.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 64)
    private String categoryCode;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;

    public Product(String sku, String name, String categoryCode) {
        this.sku = sku;
        this.name = name;
        this.categoryCode = categoryCode;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
