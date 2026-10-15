package com.example.lab.inventory.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncExecutorConfig {
    @Bean(name = "inventoryAsyncExecutor")
    public ThreadPoolTaskExecutor inventoryAsyncExecutor(
            @Value("${async.inventory.core-pool-size:4}") int corePoolSize,
            @Value("${async.inventory.max-pool-size:8}") int maxPoolSize,
            @Value("${async.inventory.queue-capacity:16}") int queueCapacity,
            @Value("${async.inventory.keep-alive-seconds:30}") int keepAliveSeconds,
            MeterRegistry meterRegistry) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("inventory-async-");
        // Rejection is intentional: it makes overload visible as backpressure instead of
        // silently running work on the servlet thread (CallerRunsPolicy).
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();

        meterRegistry.gauge("inventory.async.pool.active", executor, ThreadPoolTaskExecutor::getActiveCount);
        meterRegistry.gauge("inventory.async.pool.size", executor, ThreadPoolTaskExecutor::getPoolSize);
        meterRegistry.gauge("inventory.async.pool.queue", executor,
                e -> e.getThreadPoolExecutor().getQueue().size());
        return executor;
    }
}
