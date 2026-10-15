package com.example.lab.inventory.controller;

import com.example.lab.inventory.service.InventoryService;
import com.example.lab.inventory.async.InventoryAsyncService;
import io.micrometer.observation.annotation.Observed;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;
    private final InventoryAsyncService asyncService;

    public InventoryController(InventoryService service, InventoryAsyncService asyncService) {
        this.service = service;
        this.asyncService = asyncService;
    }

    @Observed(name = "inventory.api.list", contextualName = "inventory-list")
    @GetMapping
    public org.springframework.data.domain.Page<InventoryService.InventoryView> list(@RequestParam String warehouseCode,
                                                                                     @RequestParam(defaultValue = "0") int page,
                                                                                     @RequestParam(defaultValue = "50") int size) {
        return service.findByWarehouse(warehouseCode, org.springframework.data.domain.PageRequest.of(page, size));
    }


    @Observed(name = "inventory.api.async", contextualName = "inventory-async-fanout")
    @GetMapping("/async")
    public java.util.concurrent.CompletableFuture<ResponseEntity<InventoryAsyncService.AsyncInventoryResult>> listAsync(
            @RequestParam String warehouseCode,
            @RequestParam(defaultValue = "4") int pages,
            @RequestParam(defaultValue = "50") int size) {
        return asyncService.fetchPagesAsync(warehouseCode, pages, size)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/async/sequential")
    public ResponseEntity<InventoryAsyncService.AsyncInventoryResult> listSequential(
            @RequestParam String warehouseCode,
            @RequestParam(defaultValue = "4") int pages,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(asyncService.fetchPagesSequential(warehouseCode, pages, size));
    }

    @GetMapping("/optimized")
    public org.springframework.data.domain.Page<InventoryService.InventoryView> listOptimized(@RequestParam String warehouseCode,
                                                                                              @RequestParam(defaultValue = "0") int page,
                                                                                              @RequestParam(defaultValue = "50") int size) {
        return service.findByWarehouseOptimized(warehouseCode, org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Observed(name = "inventory.api.cached", contextualName = "inventory-cache-aside")
    @GetMapping("/cached")
    public InventoryService.CachedInventoryPage listCached(@RequestParam String warehouseCode,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "50") int size,
                                                           @RequestParam(defaultValue = "LOCKED") InventoryService.StampedeProtection protection) {
        return service.findByWarehouseCached(warehouseCode, page, size, protection);
    }

    @GetMapping("/cached/{inventoryId}")
    public InventoryService.InventoryView itemCached(@PathVariable long inventoryId) {
        return service.findItemCached(inventoryId, InventoryService.StampedeProtection.LOCKED);
    }

    @GetMapping("/keyset")
    public InventoryService.KeysetPage keyset(@RequestParam String warehouseCode,
                                              @RequestParam(required = false) Long cursor,
                                              @RequestParam(defaultValue = "50") int size) {
        return service.findByWarehouseKeyset(warehouseCode, cursor, size);
    }

    @PostMapping("/{inventoryId}/reserve/{strategy}")
    public ResponseEntity<Void> reserve(@PathVariable long inventoryId,
                                        @PathVariable String strategy,
                                        @RequestParam long amount,
                                        @RequestParam(defaultValue = "true") boolean invalidateCache) {
        switch (strategy) {
            case "none" -> service.reserveWithoutLock(inventoryId, amount, invalidateCache);
            case "optimistic" -> service.reserveOptimistic(inventoryId, amount, invalidateCache);
            case "pessimistic" -> service.reservePessimistic(inventoryId, amount, invalidateCache);
            case "atomic" -> service.reserveAtomically(inventoryId, amount, invalidateCache);
            default -> throw new IllegalArgumentException("unknown strategy: " + strategy);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cache/invalidate")
    public ResponseEntity<Void> invalidate(@RequestParam String warehouseCode) {
        // Experiment/control endpoint: emulate an explicit cache invalidation event.
        service.invalidateWarehouseCacheForExperiment(warehouseCode);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(java.util.concurrent.RejectedExecutionException.class)
    public ResponseEntity<java.util.Map<String, Object>> handleAsyncRejected() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(java.util.Map.of(
                "error", "ASYNC_BACKPRESSURE",
                "message", "inventory async executor is saturated; retry later"));
    }

}
