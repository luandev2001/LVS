package com.example.lab.inventory.async;

import com.example.lab.inventory.domain.Inventory;
import com.example.lab.inventory.repository.InventoryRepository;
import com.example.lab.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** One Spring proxy invocation = one bounded DB transaction/connection for the page read. */
@Service
public class InventoryAsyncDbReader {
    private final InventoryRepository repository;
    private final long simulatedDbDelayMs;

    public InventoryAsyncDbReader(InventoryRepository repository,
                                  @Value("${async.inventory.simulated-db-delay-ms:${cache.inventory.simulated-db-delay-ms:0}}") long simulatedDbDelayMs) {
        this.repository = repository;
        this.simulatedDbDelayMs = simulatedDbDelayMs;
    }

    @Transactional(readOnly = true)
    public InventoryAsyncService.PageChunk loadPage(String warehouseCode, int page, int size) {
        if (simulatedDbDelayMs > 0) {
            try {
                Thread.sleep(simulatedDbDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("async DB simulation interrupted", e);
            }
        }
        var result = repository.findAllByWarehouseCodeWithProduct(warehouseCode, PageRequest.of(page, size));
        List<InventoryService.InventoryView> rows = result.getContent().stream().map(this::toView).toList();
        return new InventoryAsyncService.PageChunk(page, rows);
    }

    private InventoryService.InventoryView toView(Inventory i) {
        var p = i.getProduct();
        return new InventoryService.InventoryView(i.getId(), p.getId(), p.getSku(), p.getName(),
                i.getWarehouseCode(), i.getQuantity(), i.getReservedQuantity(), i.getVersion());
    }
}
