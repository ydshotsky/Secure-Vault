package com.passwordManager.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
@Configuration
public class CpuExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService cpuPool(){
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }
}
