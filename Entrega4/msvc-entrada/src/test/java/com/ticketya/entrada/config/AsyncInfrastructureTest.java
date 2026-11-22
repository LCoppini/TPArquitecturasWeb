package com.ticketya.entrada.config;

import feign.RequestTemplate;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = {AsyncConfig.class, CorrelationIdFeignInterceptor.class,
        CorrelationIdFilter.class})
class AsyncInfrastructureTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    @Qualifier(AsyncConfig.PREFERENCIAS_TASK_EXECUTOR)
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private CorrelationIdFeignInterceptor interceptor;

    @Autowired
    private CorrelationIdFilter filter;

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void elContextoRegistraElExecutorYElInterceptor() {
        assertNotNull(context.getBean(AsyncConfig.PREFERENCIAS_TASK_EXECUTOR));
        assertNotNull(interceptor);
        assertNotNull(filter);
    }

    @Test
    void elHiloAsincronoConservaElCorrelationIdDelMdc() throws Exception {
        MDC.put(CorrelationContext.MDC_KEY, "corr-123");
        AtomicReference<String> vistoEnElHilo = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        executor.submit(() -> {
            try {
                vistoEnElHilo.set(MDC.get(CorrelationContext.MDC_KEY));
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("corr-123", vistoEnElHilo.get());
    }

    @Test
    void elInterceptorFeignEnviaElCorrelationIdComoHeader() {
        MDC.put(CorrelationContext.MDC_KEY, "corr-456");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertEquals("corr-456", template.headers().get(CorrelationContext.HEADER).iterator().next());
    }

    @Test
    void elInterceptorNoAgregaElHeaderCuandoNoHayCorrelationId() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertFalse(template.headers().containsKey(CorrelationContext.HEADER));
    }

    @Test
    void elFiltroPoneElCorrelationIdEnElMDCYLoLimpiaAlTerminar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationContext.HEADER, "corr-789");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> vistoEnLaCadena = new AtomicReference<>();

        FilterChain cadena = (req, resp) -> vistoEnLaCadena.set(MDC.get(CorrelationContext.MDC_KEY));
        filter.doFilter(request, response, cadena);

        assertEquals("corr-789", vistoEnLaCadena.get());
        assertEquals("corr-789", response.getHeader(CorrelationContext.HEADER));
        assertEquals(null, MDC.get(CorrelationContext.MDC_KEY));
    }
}
