package com.ticketya.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Correlaciona la peticion que entra por el gateway con la reenviada a los
 * servicios y con la respuesta, incluso cuando el error lo genera el propio
 * gateway. Se ejecuta antes de Spring Security para cubrir tambien los 401/403.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String recibido = request.getHeader(HEADER);
        String correlationId = recibido == null || recibido.isBlank()
                ? UUID.randomUUID().toString()
                : recibido;

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    return HEADER.equalsIgnoreCase(name) ? correlationId : super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    return HEADER.equalsIgnoreCase(name)
                            ? Collections.enumeration(List.of(correlationId))
                            : super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    Set<String> nombres = new LinkedHashSet<>();
                    Enumeration<String> originales = super.getHeaderNames();
                    while (originales.hasMoreElements()) {
                        String nombre = originales.nextElement();
                        if (!HEADER.equalsIgnoreCase(nombre)) {
                            nombres.add(nombre);
                        }
                    }
                    nombres.add(HEADER);
                    return Collections.enumeration(nombres);
                }
            }, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
