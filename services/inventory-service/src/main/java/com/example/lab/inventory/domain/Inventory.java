package com.example.lab.inventory.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "warehouse_code", nullable = false, length = 64)
    private String warehouseCode;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private long reservedQuantity;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Inventory() {}

    public Inventory(Product product, String warehouseCode, long quantity) {
        this.product = product;
        this.warehouseCode = warehouseCode;
        this.quantity = quantity;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public String getWarehouseCode() { return warehouseCode; }
    public long getQuantity() { return quantity; }
    public long getReservedQuantity() { return reservedQuantity; }
    public long getVersion() { return version; }
    public void reserve(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (quantity - reservedQuantity < amount) throw new IllegalStateException("insufficient available stock");
        reservedQuantity += amount;
    }
}
