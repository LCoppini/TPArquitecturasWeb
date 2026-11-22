package com.ticketya.entrada.config;

/** Claves compartidas por el filtro HTTP, el MDC y el interceptor Feign. */
public final class CorrelationContext {

    public static final String MDC_KEY = "correlationId";
    public static final String HEADER = "X-Correlation-Id";

    private CorrelationContext() {
    }
}
