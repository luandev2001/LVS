package com.example.lab.inventory.repository;

import com.example.lab.inventory.domain.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Page<Inventory> findAllByWarehouseCode(String warehouseCode, Pageable pageable);

    @Query(value = "select i from Inventory i join fetch i.product where i.warehouseCode = :warehouseCode", countQuery = "select count(i) from Inventory i where i.warehouseCode = :warehouseCode")
    Page<Inventory> findAllByWarehouseCodeWithProduct(@Param("warehouseCode") String warehouseCode, Pageable pageable);

    @Query("select i from Inventory i join fetch i.product where i.warehouseCode = :warehouseCode and (i.id < :cursor or :cursor is null) order by i.id desc")
    List<Inventory> findKeyset(@Param("warehouseCode") String warehouseCode, @Param("cursor") Long cursor, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.id = :id")
    Optional<Inventory> findByIdForUpdate(@Param("id") Long id);
}
