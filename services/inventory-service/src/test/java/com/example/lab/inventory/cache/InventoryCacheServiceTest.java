package com.example.lab.inventory.cache;

import com.example.lab.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryCacheServiceTest {
    @Test
    void warehouseKeyUsesRedisNamespaceVersion() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("inventory:cache:version:WH-1")).thenReturn("7");

        InventoryCacheService service = new InventoryCacheService(
                redis, new ObjectMapper(), new SimpleMeterRegistry(), 30, 5000, 2000, true, true);

        assertEquals("inventory:cache:data:WH-1:v7:page=0:size=50",
                service.warehouseKey("WH-1", 0, 50));
    }

    @Test
    void cacheValueIsSerializedWithTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        InventoryCacheService service = new InventoryCacheService(
                redis, new ObjectMapper(), new SimpleMeterRegistry(), 30, 5000, 2000, true, true);

        InventoryService.CachedInventoryPage page = new InventoryService.CachedInventoryPage(
                List.of(new InventoryService.InventoryView(1L, 1L, "SKU-1", "Product 1", "WH-1", 10, 2, 0)),
                0, 50, 1, 1);

        service.put("inventory:cache:data:WH-1:v0:page=0:size=50", page);

        verify(values).set(eq("inventory:cache:data:WH-1:v0:page=0:size=50"), anyString(), eq(java.time.Duration.ofSeconds(30)));
    }
}
