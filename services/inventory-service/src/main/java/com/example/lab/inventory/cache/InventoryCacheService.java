package com.example.lab.inventory.cache;

import com.example.lab.inventory.service.InventoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class InventoryCacheService {
    private static final String VERSION_PREFIX = "inventory:cache:version:";
    private static final String DATA_PREFIX = "inventory:cache:data:";
    private static final String LOCK_PREFIX = "inventory:cache:lock:";
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${cache.inventory.ttl-seconds:30s}")
    private Duration ttl;
    @Value("${cache.inventory.lock-ttl-ms:5000ms}")
    private Duration lockTtl;
    @Value("${cache.inventory.lock-wait-ms:2000ms}")
    private Duration lockWait;
    @Value("${cache.inventory.enabled:true}")
    private boolean enabled;
    @Value("${cache.inventory.invalidation-enabled:true}")
    private boolean invalidationEnabled;

    public Optional<InventoryService.CachedInventoryPage> get(String key) {
        if (!enabled) return Optional.empty();
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                counter("inventory.cache.miss").increment();
                return Optional.empty();
            }
            counter("inventory.cache.hit").increment();
            return Optional.of(objectMapper.readValue(json, InventoryService.CachedInventoryPage.class));
        } catch (Exception e) {
            counter("inventory.cache.error").increment();
            return Optional.empty();
        }
    }

    public void put(String key, InventoryService.CachedInventoryPage value) {
        if (!enabled) return;
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
            counter("inventory.cache.write").increment();
        } catch (JsonProcessingException | RuntimeException e) {
            counter("inventory.cache.write.error").increment();
        }
    }

    public String warehouseKey(String warehouseCode, int page, int size) {
        try {
            String version = Optional.ofNullable(redis.opsForValue().get(VERSION_PREFIX + warehouseCode)).orElse("0");
            return DATA_PREFIX + warehouseCode + ":v" + version + ":page=" + page + ":size=" + size;
        } catch (RuntimeException e) {
            counter("inventory.cache.key.error").increment();
            return DATA_PREFIX + warehouseCode + ":fallback:page=" + page + ":size=" + size;
        }
    }

    public void invalidateWarehouse(String warehouseCode) {
        if (!enabled || !invalidationEnabled) {
            counter("inventory.cache.invalidation.skipped").increment();
            return;
        }
        try {
            redis.opsForValue().increment(VERSION_PREFIX + warehouseCode);
            counter("inventory.cache.invalidation").increment();
        } catch (RuntimeException e) {
            counter("inventory.cache.invalidation.error").increment();
        }
    }

    public void invalidateItem(long inventoryId) {
        if (!enabled || !invalidationEnabled) {
            counter("inventory.cache.item-invalidation.skipped").increment();
            return;
        }
        try {
            redis.delete(DATA_PREFIX + "item:" + inventoryId);
            counter("inventory.cache.item-invalidation").increment();
        } catch (RuntimeException e) {
            counter("inventory.cache.item-invalidation.error").increment();
        }
    }

    public Optional<InventoryService.InventoryView> getItem(long inventoryId) {
        if (!enabled) return Optional.empty();
        try {
            String json = redis.opsForValue().get(DATA_PREFIX + "item:" + inventoryId);
            if (json == null) {
                counter("inventory.item-cache.miss").increment();
                return Optional.empty();
            }
            counter("inventory.item-cache.hit").increment();
            return Optional.of(objectMapper.readValue(json, InventoryService.InventoryView.class));
        } catch (Exception e) {
            counter("inventory.item-cache.error").increment();
            return Optional.empty();
        }
    }

    public void putItem(long inventoryId, InventoryService.InventoryView value) {
        if (!enabled) return;
        try {
            redis.opsForValue().set(DATA_PREFIX + "item:" + inventoryId, objectMapper.writeValueAsString(value), ttl);
            counter("inventory.item-cache.write").increment();
        } catch (JsonProcessingException | RuntimeException e) {
            counter("inventory.item-cache.write.error").increment();
        }
    }

    public LockAttempt tryAcquire(String cacheKey) {
        if (!enabled) return LockAttempt.ofUnavailable();
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(LOCK_PREFIX + cacheKey, token, lockTtl);
            if (Boolean.TRUE.equals(acquired)) {
                counter("inventory.cache.stampede.lock.acquired").increment();
                return LockAttempt.acquired(new LockHandle(cacheKey, token));
            }
            counter("inventory.cache.stampede.lock.contended").increment();
            return LockAttempt.ofContended();
        } catch (RuntimeException e) {
            counter("inventory.cache.stampede.lock.error").increment();
            return LockAttempt.ofUnavailable();
        }
    }

    public Optional<InventoryService.CachedInventoryPage> waitForValue(String cacheKey) {
        long deadline = System.nanoTime() + lockWait.toNanos();
        while (System.nanoTime() < deadline) {
            Optional<InventoryService.CachedInventoryPage> value = get(cacheKey);
            if (value.isPresent()) {
                counter("inventory.cache.stampede.wait.success").increment();
                return value;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        counter("inventory.cache.stampede.wait.timeout").increment();
        return Optional.empty();
    }

    public void release(LockHandle handle) {
        try {
            redis.execute(RELEASE_LOCK, List.of(LOCK_PREFIX + handle.cacheKey()), handle.token());
        } catch (RuntimeException e) {
            counter("inventory.cache.stampede.lock.release.error").increment();
        }
    }

    public void resetWarehouseVersion(String warehouseCode) {
        try {
            redis.delete(VERSION_PREFIX + warehouseCode);
        } catch (RuntimeException e) {
            counter("inventory.cache.version.reset.error").increment();
        }
    }

    public record LockHandle(String cacheKey, String token) {
    }

    public record LockAttempt(LockHandle handle, boolean contended, boolean available) {
        public static LockAttempt acquired(LockHandle h) {
            return new LockAttempt(h, false, true);
        }

        public static LockAttempt ofContended() {
            return new LockAttempt(null, true, true);
        }

        public static LockAttempt ofUnavailable() {
            return new LockAttempt(null, false, false);
        }
    }

    private io.micrometer.core.instrument.Counter counter(String name) {
        return meterRegistry.counter(name);
    }
}
