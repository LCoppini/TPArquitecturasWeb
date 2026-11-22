package com.ticketya.entrada.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Valores por defecto coincidentes con el diseño; se sobreescriben desde msvc-entrada.yml. */
@Component
@ConfigurationProperties(prefix = "ticketya.preferencias.outbox")
public class OutboxProperties {

    private int intentosMaximos = 5;
    private List<Duration> backoff = new ArrayList<>(List.of(
            Duration.ofSeconds(10), Duration.ofSeconds(30), Duration.ofMinutes(1), Duration.ofMinutes(5)));
    private Duration staleTimeout = Duration.ofMinutes(5);
    private Duration pollMs = Duration.ofSeconds(5);

    public int getIntentosMaximos() {
        return intentosMaximos;
    }

    public void setIntentosMaximos(int intentosMaximos) {
        this.intentosMaximos = intentosMaximos;
    }

    public List<Duration> getBackoff() {
        return backoff;
    }

    public void setBackoff(List<Duration> backoff) {
        this.backoff = backoff;
    }

    public Duration getStaleTimeout() {
        return staleTimeout;
    }

    public void setStaleTimeout(Duration staleTimeout) {
        this.staleTimeout = staleTimeout;
    }

    public Duration getPollMs() {
        return pollMs;
    }

    public void setPollMs(Duration pollMs) {
        this.pollMs = pollMs;
    }

    /** Espera después del fallo número "intentos" (1-based). */
    public Duration esperaDespuesDe(int intentos) {
        if (backoff == null || backoff.isEmpty()) {
            return Duration.ZERO;
        }
        int indice = Math.min(intentos - 1, backoff.size() - 1);
        return backoff.get(Math.max(indice, 0));
    }
}
