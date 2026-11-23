package com.ticketya.evento.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Executor propio de los avisos que evento envia a otros servicios despues del commit. */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String AVISOS_TASK_EXECUTOR = "avisosTaskExecutor";

    @Bean(name = AVISOS_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor avisosTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("avisos-");
        executor.initialize();
        return executor;
    }
}
