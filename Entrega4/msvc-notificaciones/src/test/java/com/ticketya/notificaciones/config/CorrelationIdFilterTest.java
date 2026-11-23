package com.ticketya.notificaciones.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void tomaElHeaderLoPoneEnElMdcYLoDevuelve() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> visto = new AtomicReference<>();
        FilterChain cadena = (req, res) -> visto.set(MDC.get("correlationId"));

        filtro.doFilter(request, response, cadena);

        assertEquals("corr-123", visto.get());
        assertEquals("corr-123", response.getHeader("X-Correlation-Id"));
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void siNoVieneElHeaderGeneraUno() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> visto = new AtomicReference<>();
        FilterChain cadena = (req, res) -> visto.set(MDC.get("correlationId"));

        filtro.doFilter(request, response, cadena);

        assertNotNull(visto.get());
        assertFalse(visto.get().isBlank());
        assertEquals(visto.get(), response.getHeader("X-Correlation-Id"));
    }
}
