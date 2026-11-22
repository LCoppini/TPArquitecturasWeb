package com.ticketya.entrada.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** Executor propio del worker de preferencias, con propagación del MDC. */
@Configuration
public class AsyncConfig {

    public static final String PREFERENCIAS_TASK_EXECUTOR = "preferenciasTaskExecutor";

    @Bean(name = PREFERENCIAS_TASK_EXECUTOR)
    public ThreadPoolTaskExecutor preferenciasTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("preferencias-worker-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean(name = "preferenciasTimeLimiterScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService preferenciasTimeLimiterScheduler() {
        return Executors.newSingleThreadScheduledExecutor(tarea -> {
            Thread hilo = new Thread(tarea, "preferencias-timelimiter");
            hilo.setDaemon(true);
            return hilo;
        });
    }
}
