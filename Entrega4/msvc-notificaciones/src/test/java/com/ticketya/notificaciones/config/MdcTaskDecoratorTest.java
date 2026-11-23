package com.ticketya.notificaciones.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MdcTaskDecoratorTest {

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void elHiloDelPoolVeElCorrelationIdDelHiloQueEncolo() throws Exception {
        MDC.put("correlationId", "corr-123");
        AtomicReference<String> visto = new AtomicReference<>();
        Runnable decorada = new MdcTaskDecorator().decorate(() -> visto.set(MDC.get("correlationId")));
        MDC.clear();

        Thread hilo = new Thread(decorada);
        hilo.start();
        hilo.join();

        assertEquals("corr-123", visto.get());
    }

    @Test
    void elContextoNoSeFiltraAlSiguienteUsoDelHilo() {
        MDC.put("correlationId", "corr-123");
        Runnable decorada = new MdcTaskDecorator().decorate(() -> { });
        MDC.clear();
        MDC.put("correlationId", "otro-hilo");

        decorada.run();

        assertEquals("otro-hilo", MDC.get("correlationId"));
    }

    @Test
    void sinContextoElHiloNoHeredaNada() {
        Runnable decorada = new MdcTaskDecorator().decorate(() -> assertNull(MDC.get("correlationId")));
        MDC.put("correlationId", "residuo");

        decorada.run();

        assertEquals("residuo", MDC.get("correlationId"));
    }
}
