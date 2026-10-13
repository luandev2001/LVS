package com.example.lab.inventory.repository;

import com.example.lab.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryAtomicRepository extends JpaRepository<Inventory, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update inventory set reserved_quantity = reserved_quantity + :amount, updated_at = current_timestamp where id = :id and quantity - reserved_quantity >= :amount", nativeQuery = true)
    int reserveAtomically(@Param("id") long id, @Param("amount") long amount);
}
