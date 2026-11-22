package com.ticketya.entrada.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** Propaga el correlationId del MDC como X-Correlation-Id en cada llamada Feign. */
@Component
public class CorrelationIdFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String correlationId = MDC.get(CorrelationContext.MDC_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            template.header(CorrelationContext.HEADER, correlationId);
        }
    }
}
