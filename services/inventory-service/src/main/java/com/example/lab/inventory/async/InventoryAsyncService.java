package com.example.lab.inventory.async;

import com.example.lab.inventory.service.InventoryService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
public class InventoryAsyncService {
    private final InventoryAsyncDbReader dbReader;
    private final Executor executor;
    private final MeterRegistry metrics;

    public InventoryAsyncService(InventoryAsyncDbReader dbReader,
                                 @Qualifier("inventoryAsyncExecutor") Executor executor,
                                 MeterRegistry metrics) {
        this.dbReader = dbReader;
        this.executor = executor;
        this.metrics = metrics;
    }

    /**
     * Fan-out/fan-in example. Each page is a separate DB query and can execute concurrently.
     * The bounded executor is the backpressure boundary: rejection is returned to the caller
     * rather than using the servlet thread as an accidental overflow worker.
     */
    public CompletableFuture<AsyncInventoryResult> fetchPagesAsync(String warehouseCode, int pages, int size) {
        long started = System.nanoTime();
        List<CompletableFuture<PageChunk>> futures = new ArrayList<>(pages);
        try {
            for (int page = 0; page < pages; page++) {
                final int requestedPage = page;
                futures.add(CompletableFuture.supplyAsync(
                        () -> loadPage(warehouseCode, requestedPage, size), executor));
            }
        } catch (RejectedExecutionException rejected) {
            futures.forEach(f -> f.cancel(true));
            metrics.counter("inventory.async.rejected").increment();
            throw rejected;
        }

        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        return all.thenApply(ignored -> {
            List<InventoryService.InventoryView> rows = futures.stream()
                    .flatMap(f -> f.join().rows().stream())
                    .toList();
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            metrics.timer("inventory.async.fanout").record(elapsedMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            return new AsyncInventoryResult(warehouseCode, pages, size, rows, elapsedMs,
                    Math.min(pages, executorActivePoolSize()));
        });
    }

    /**
     * Sequential control group: same DB work, one query after another.
     */
    public AsyncInventoryResult fetchPagesSequential(String warehouseCode, int pages, int size) {
        long started = System.nanoTime();
        List<InventoryService.InventoryView> rows = new ArrayList<>();
        for (int page = 0; page < pages; page++) {
            rows.addAll(loadPage(warehouseCode, page, size).rows());
        }
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        return new AsyncInventoryResult(warehouseCode, pages, size, rows, elapsedMs, 1);
    }

    public PageChunk loadPage(String warehouseCode, int page, int size) {
        return dbReader.loadPage(warehouseCode, page, size);
    }


    private int executorActivePoolSize() {
        if (executor instanceof org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor e) {
            return e.getActiveCount();
        }
        return -1;
    }

    public record PageChunk(int page, List<InventoryService.InventoryView> rows) {
    }

    public record AsyncInventoryResult(String warehouseCode, int pages, int size,
                                       List<InventoryService.InventoryView> content,
                                       long elapsedMs, int activeWorkersAtCompletion) {
    }
}
