package com.example.lab.inventory.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Small, deliberately explicit resilience implementation for experiments; not a replacement for a mature library.
 */
@Service
public class ResilienceLabService {
    public enum CircuitState {CLOSED, OPEN, HALF_OPEN}

    private final Semaphore bulkhead;
    private final int failureThreshold;
    private final long openMillis;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile CircuitState state = CircuitState.CLOSED;
    private volatile long openedAt;
    private final Object circuitLock = new Object();

    public ResilienceLabService(
            @Value("${resilience.lab.bulkhead-permits:8}") int permits,
            @Value("${resilience.lab.failure-threshold:3}") int threshold,
            @Value("${resilience.lab.open-ms:5000}") long openMillis) {
        this.bulkhead = new Semaphore(Math.max(1, permits));
        this.failureThreshold = Math.max(1, threshold);
        this.openMillis = Math.max(1, openMillis);
    }

    public Result call(boolean fail, long delayMs, int maxRetries) {
        long started = System.nanoTime();
        if (!allowRequest()) throw new CircuitOpenException("circuit is OPEN");
        if (!bulkhead.tryAcquire()) throw new BulkheadFullException("bulkhead capacity exhausted");
        int attempts = 0;
        try {
            while (true) {
                attempts++;
                try {
                    if (delayMs > 0) Thread.sleep(Math.min(delayMs, 30_000));
                    if (fail) throw new IllegalStateException("injected downstream failure");
                    onSuccess();
                    return new Result("SUCCESS", attempts, state.name(), (System.nanoTime() - started) / 1_000_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onFailure();
                    throw new IllegalStateException("interrupted", e);
                } catch (RuntimeException e) {
                    onFailure();
                    if (attempts > Math.max(0, maxRetries)) throw e;
                    try {
                        Thread.sleep(Math.min(50L * attempts, 500));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(interrupted);
                    }
                    if (state == CircuitState.OPEN) throw new CircuitOpenException("circuit opened during retry");
                }
            }
        } finally {
            bulkhead.release();
        }
    }

    private boolean allowRequest() {
        synchronized (circuitLock) {
            if (state == CircuitState.CLOSED) return true;
            if (state == CircuitState.OPEN && System.currentTimeMillis() - openedAt >= openMillis) {
                state = CircuitState.HALF_OPEN;
                consecutiveFailures.set(0);
                return true;
            }
            return state == CircuitState.HALF_OPEN && consecutiveFailures.get() == 0;
        }
    }

    private void onSuccess() {
        synchronized (circuitLock) {
            consecutiveFailures.set(0);
            state = CircuitState.CLOSED;
        }
    }

    private void onFailure() {
        synchronized (circuitLock) {
            if (consecutiveFailures.incrementAndGet() >= failureThreshold || state == CircuitState.HALF_OPEN) {
                state = CircuitState.OPEN;
                openedAt = System.currentTimeMillis();
            }
        }
    }

    public record Result(String status, int attempts, String circuitState, long elapsedMs) {
    }

    public static class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String m) {
            super(m);
        }
    }

    public static class BulkheadFullException extends RuntimeException {
        public BulkheadFullException(String m) {
            super(m);
        }
    }
}
