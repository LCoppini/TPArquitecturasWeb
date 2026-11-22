package com.ticketya.entrada.service.impl;

import com.ticketya.entrada.client.PreferenciasClient;
import com.ticketya.entrada.config.AsyncConfig;
import com.ticketya.entrada.dto.request.CompraRegistradaRequest;
import com.ticketya.entrada.service.PreferenciasGateway;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/** Circuito y time limit configurados con el nombre "preferencias" (ver msvc-entrada.yml). */
@Component
public class ResilientPreferenciasGateway implements PreferenciasGateway {

    public static final String NOMBRE_CIRCUITO = "preferencias";

    private final PreferenciasClient client;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final Executor executor;
    private final ScheduledExecutorService timeLimiterScheduler;

    public ResilientPreferenciasGateway(
            PreferenciasClient client,
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            @Qualifier(AsyncConfig.PREFERENCIAS_TASK_EXECUTOR) Executor executor,
            @Qualifier("preferenciasTimeLimiterScheduler") ScheduledExecutorService timeLimiterScheduler
    ) {
        this.client = client;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
        this.executor = executor;
        this.timeLimiterScheduler = timeLimiterScheduler;
    }

    @Override
    public void registrar(CompraRegistradaRequest compra) {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(NOMBRE_CIRCUITO);
        TimeLimiter limitador = timeLimiterRegistry.timeLimiter(NOMBRE_CIRCUITO);

        Supplier<CompletionStage<Void>> llamada = () -> CompletableFuture.supplyAsync(() -> {
            client.registrar(compra);
            return (Void) null;
        }, executor);
        Supplier<CompletionStage<Void>> decorada = TimeLimiter.decorateCompletionStage(
                limitador,
                timeLimiterScheduler,
                CircuitBreaker.decorateCompletionStage(breaker, llamada));

        try {
            decorada.get().toCompletableFuture().get();
        } catch (CallNotPermittedException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Llamada a preferencias interrumpida", exception);
        } catch (ExecutionException exception) {
            Throwable causa = exception.getCause();
            if (causa instanceof CallNotPermittedException callNotPermitted) {
                throw callNotPermitted;
            }
            if (causa instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Fallo al registrar la compra en preferencias", causa);
        }
    }
}
