package com.example.lab.inventory.domain;

import jakarta.persistence.*;
import java.time.Instant;

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

    protected Product() {}

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

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getCategoryCode() { return categoryCode; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
