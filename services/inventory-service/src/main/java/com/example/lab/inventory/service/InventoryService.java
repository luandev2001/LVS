package com.example.lab.inventory.service;

import com.example.lab.inventory.cache.InventoryCacheService;
import com.example.lab.inventory.domain.Inventory;
import com.example.lab.inventory.domain.Product;
import com.example.lab.inventory.repository.InventoryAtomicRepository;
import com.example.lab.inventory.repository.InventoryRepository;
import com.example.lab.inventory.repository.ProductRepository;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryAtomicRepository atomicRepository;
    private final ProductRepository productRepository;
    private final InventoryCacheService cache;
    private final long simulatedDbDelayMs;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryAtomicRepository atomicRepository,
                            ProductRepository productRepository,
                            InventoryCacheService cache,
                            @Value("${cache.inventory.simulated-db-delay-ms:0}") long simulatedDbDelayMs) {
        this.inventoryRepository = inventoryRepository;
        this.atomicRepository = atomicRepository;
        this.productRepository = productRepository;
        this.cache = cache;
        this.simulatedDbDelayMs = simulatedDbDelayMs;
    }

    @Transactional(readOnly = true)
    public Page<InventoryView> findByWarehouse(String warehouseCode, Pageable pageable) {
        return inventoryRepository.findAllByWarehouseCode(warehouseCode, pageable).map(this::toView);
    }

    @Transactional(readOnly = true)
    public Page<InventoryView> findByWarehouseOptimized(String warehouseCode, Pageable pageable) {
        return inventoryRepository.findAllByWarehouseCodeWithProduct(warehouseCode, pageable).map(this::toView);
    }

    /**
     * Cache-aside: read Redis first, then optimized PostgreSQL query on miss.
     */
    @Timed(value = "inventory.cache.api", extraTags = {"mode", "cache-aside"})
    public CachedInventoryPage findByWarehouseCached(String warehouseCode, int page, int size, StampedeProtection protection) {
        String key = cache.warehouseKey(warehouseCode, page, size);
        Optional<CachedInventoryPage> hit = cache.get(key);
        if (hit.isPresent()) return hit.get();

        if (protection == StampedeProtection.LOCKED) {
            InventoryCacheService.LockAttempt attempt = cache.tryAcquire(key);
            if (attempt.handle() != null) {
                try {
                    // Double-check after acquiring the lock: another request may have filled it just before us.
                    Optional<CachedInventoryPage> secondCheck = cache.get(key);
                    if (secondCheck.isPresent()) return secondCheck.get();
                    CachedInventoryPage loaded = loadAndCache(warehouseCode, page, size, true);
                    cache.put(key, loaded);
                    return loaded;
                } finally {
                    cache.release(attempt.handle());
                }
            }
            if (attempt.contended()) {
                Optional<CachedInventoryPage> waited = cache.waitForValue(key);
                if (waited.isPresent()) return waited.get();
            }
        }

        // NAIVE mode, or cache/lock infrastructure unavailable: every caller can hit PostgreSQL.
        return loadAndCache(warehouseCode, page, size, true);
    }

    private CachedInventoryPage loadAndCache(String warehouseCode, int page, int size, boolean simulateDelay) {
        if (simulateDelay && simulatedDbDelayMs > 0) {
            try {
                Thread.sleep(simulatedDbDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Page<InventoryView> result = inventoryRepository
                .findAllByWarehouseCodeWithProduct(warehouseCode, org.springframework.data.domain.PageRequest.of(page, size))
                .map(this::toView);
        return new CachedInventoryPage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public InventoryView findItemCached(long inventoryId, StampedeProtection protection) {
        Optional<InventoryView> hit = cache.getItem(inventoryId);
        if (hit.isPresent()) return hit.get();
        Inventory inventory = inventoryRepository.findById(inventoryId).orElseThrow();
        InventoryView view = toView(inventory);
        cache.putItem(inventoryId, view);
        return view;
    }

    @Transactional(readOnly = true)
    public KeysetPage findByWarehouseKeyset(String warehouseCode, Long cursor, int size) {
        List<Inventory> rows = inventoryRepository.findKeyset(warehouseCode, cursor, org.springframework.data.domain.PageRequest.of(0, size + 1));
        boolean hasNext = rows.size() > size;
        if (hasNext) rows = rows.subList(0, size);
        Long nextCursor = rows.isEmpty() ? null : rows.get(rows.size() - 1).getId();
        return new KeysetPage(rows.stream().map(this::toView).toList(), nextCursor, hasNext);
    }

    @Transactional
    public void reserveWithoutLock(long inventoryId, long amount) {
        reserveWithoutLock(inventoryId, amount, true);
    }

    @Transactional
    public void reserveWithoutLock(long inventoryId, long amount, boolean invalidateCache) {
        Inventory inventory = inventoryRepository.findById(inventoryId).orElseThrow();
        inventory.reserve(amount);
        inventoryRepository.save(inventory);
        invalidateAfterMutation(inventoryId, inventory.getWarehouseCode(), invalidateCache);
    }

    @Transactional
    public void reserveOptimistic(long inventoryId, long amount) {
        reserveOptimistic(inventoryId, amount, true);
    }

    @Transactional
    public void reserveOptimistic(long inventoryId, long amount, boolean invalidateCache) {
        Inventory inventory = inventoryRepository.findById(inventoryId).orElseThrow();
        inventory.reserve(amount);
        try {
            inventoryRepository.saveAndFlush(inventory);
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentReservationException("optimistic locking conflict", e);
        }
        invalidateAfterMutation(inventoryId, inventory.getWarehouseCode(), invalidateCache);
    }

    @Transactional
    public void reservePessimistic(long inventoryId, long amount) {
        reservePessimistic(inventoryId, amount, true);
    }

    @Transactional
    public void reservePessimistic(long inventoryId, long amount, boolean invalidateCache) {
        Inventory inventory = inventoryRepository.findByIdForUpdate(inventoryId).orElseThrow();
        inventory.reserve(amount);
        inventoryRepository.save(inventory);
        invalidateAfterMutation(inventoryId, inventory.getWarehouseCode(), invalidateCache);
    }

    @Transactional
    public void reserveAtomically(long inventoryId, long amount) {
        reserveAtomically(inventoryId, amount, true);
    }

    @Transactional
    public void reserveAtomically(long inventoryId, long amount, boolean invalidateCache) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (atomicRepository.reserveAtomically(inventoryId, amount) != 1) {
            throw new IllegalStateException("insufficient available stock or inventory not found");
        }
        String warehouse = inventoryRepository.findById(inventoryId).orElseThrow().getWarehouseCode();
        invalidateAfterMutation(inventoryId, warehouse, invalidateCache);
    }

    public void invalidateWarehouseCacheForExperiment(String warehouseCode) {
        cache.invalidateWarehouse(warehouseCode);
    }

    private void invalidateAfterMutation(long inventoryId, String warehouseCode, boolean invalidateCache) {
        if (!invalidateCache) return;
        Runnable invalidation = () -> {
            cache.invalidateItem(inventoryId);
            cache.invalidateWarehouse(warehouseCode);
        };
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            invalidation.run();
                        }
                    });
        } else {
            invalidation.run();
        }
    }

    private InventoryView toView(Inventory i) {
        Product p = i.getProduct();
        return new InventoryView(i.getId(), p.getId(), p.getSku(), p.getName(), i.getWarehouseCode(),
                i.getQuantity(), i.getReservedQuantity(), i.getVersion());
    }

    public record InventoryView(Long inventoryId, Long productId, String sku, String productName,
                                String warehouseCode, long quantity, long reservedQuantity, long version) {
    }

    public record CachedInventoryPage(List<InventoryView> content, int page, int size,
                                      long totalElements, int totalPages) {
    }

    public record KeysetPage(List<InventoryView> content, Long nextCursor, boolean hasNext) {
    }

    public enum StampedeProtection {NAIVE, LOCKED}

    public static class ConcurrentReservationException extends RuntimeException {
        public ConcurrentReservationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
