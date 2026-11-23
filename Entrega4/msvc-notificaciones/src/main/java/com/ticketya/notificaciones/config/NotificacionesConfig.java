package com.ticketya.notificaciones.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;

/**
 * Envio asincrono (pool propio de hilos con @Async) y worker de reintentos (@Scheduled).
 * El correlationId se copia al hilo con MdcTaskDecorator.
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(NotificacionesProperties.class)
public class NotificacionesConfig {

    public static final String EXECUTOR = "notificacionesExecutor";

    @Bean(name = EXECUTOR)
    public ThreadPoolTaskExecutor notificacionesExecutor(NotificacionesProperties propiedades) {
        NotificacionesProperties.Executor config = propiedades.getExecutor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCore());
        executor.setMaxPoolSize(config.getMax());
        executor.setQueueCapacity(config.getCola());
        executor.setThreadNamePrefix("notif-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        return executor;
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
