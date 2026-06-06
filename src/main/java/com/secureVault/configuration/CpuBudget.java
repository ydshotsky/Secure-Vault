package com.secureVault.configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class CpuBudget {

    private final Semaphore semaphore;
    private final int totalPermits;
    private final Counter totalAcquisitions;

    public CpuBudget(@Value("${CPU_LIMIT}") int permits, MeterRegistry meterRegistry) {
        this.totalPermits = permits;
        this.semaphore = new Semaphore(permits, true);

        this.totalAcquisitions = Counter.builder("cpu.budget.acquisitions")
                .description("Total number of times a CPU budget permit was successfully acquired")
                .register(meterRegistry);

        Gauge.builder("cpu.budget.active.permits", () -> totalPermits - semaphore.availablePermits())
                .description("Current number of active CPU budget permits in use")
                .register(meterRegistry);

        log.info("CPU Budget Initialized. Configured Permits: {}, Available CPU Cores: {}", 
                permits, Runtime.getRuntime().availableProcessors());
    }

    public void acquire() throws InterruptedException {
        semaphore.acquire();
        totalAcquisitions.increment();
        log.info("CPU Budget permit acquired. In-use: {}/{}", 
                totalPermits - semaphore.availablePermits(), totalPermits);
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        boolean acquired = semaphore.tryAcquire(timeout, unit);
        if (acquired) {
            totalAcquisitions.increment();
            log.info("CPU Budget permit acquired (tryAcquire). In-use: {}/{}", 
                    totalPermits - semaphore.availablePermits(), totalPermits);
        }
        return acquired;
    }

    public void release() {
        semaphore.release();
        log.info("CPU Budget permit released. In-use: {}/{}", 
                totalPermits - semaphore.availablePermits(), totalPermits);
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }
}
