package com.ticketya.entrada.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que el YAML publicado por msvc-config bindea exactamente a las propiedades
 * que consumen el worker y los componentes de resiliencia. No levanta el config server:
 * lee el mismo archivo que éste publica (ADR-0002, sin contenedores adicionales).
 */
class ResilienciaConfigBindingTest {

    private static final String YAML_CONFIG_SERVER =
            "../msvc-config/src/main/resources/configuration/msvc-entrada.yml";

    @Test
    void outboxBindeaLosCincoValoresDelDiseño() throws IOException {
        OutboxProperties properties = binder().bind(
                "ticketya.preferencias.outbox", OutboxProperties.class).get();

        assertEquals(5, properties.getIntentosMaximos());
        assertEquals(List.of(Duration.ofSeconds(10), Duration.ofSeconds(30),
                Duration.ofMinutes(1), Duration.ofMinutes(5)), properties.getBackoff());
        assertEquals(Duration.ofMinutes(5), properties.getStaleTimeout());
        assertEquals(Duration.ofSeconds(5), properties.getPollMs());
    }

    @Test
    void circuitBreakerBindeaLaVentanaDeTiempoYLaTransicionSemiAbierta() throws IOException {
        Binder binder = binder();
        String prefijo = "resilience4j.circuitbreaker.instances.preferencias.";

        assertEquals("TIME_BASED", binder.bind(prefijo + "sliding-window-type", String.class).get());
        assertEquals(120, binder.bind(prefijo + "sliding-window-size", Integer.class).get());
        assertEquals(3, binder.bind(prefijo + "minimum-number-of-calls", Integer.class).get());
        assertEquals(50, binder.bind(prefijo + "failure-rate-threshold", Integer.class).get());
        assertEquals(Duration.ofSeconds(30),
                binder.bind(prefijo + "wait-duration-in-open-state", Duration.class).get());
        assertTrue(binder.bind(prefijo + "automatic-transition-from-open-to-half-open-enabled", Boolean.class).get());
    }

    @Test
    void timeLimiterYFeignBindeanSinRetryInterno() throws IOException {
        Binder binder = binder();

        assertEquals(Duration.ofSeconds(2), binder.bind(
                "resilience4j.timelimiter.instances.preferencias.timeout-duration", Duration.class).get());
        assertEquals(500, binder.bind(
                "spring.cloud.openfeign.client.config.msvc-preferencias.connect-timeout", Integer.class).get());
        assertEquals(1200, binder.bind(
                "spring.cloud.openfeign.client.config.msvc-preferencias.read-timeout", Integer.class).get());
        assertFalse(binder.bind("resilience4j.retry", Bindable.mapOf(String.class, Object.class)).isBound());
    }

    private Binder binder() throws IOException {
        var fuentes = new YamlPropertySourceLoader()
                .load("msvc-entrada-config-server", new FileSystemResource(YAML_CONFIG_SERVER));
        return new Binder(ConfigurationPropertySources.from(fuentes));
    }
}
