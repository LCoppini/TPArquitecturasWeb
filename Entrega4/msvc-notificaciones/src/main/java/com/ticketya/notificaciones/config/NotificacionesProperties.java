package com.ticketya.notificaciones.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Parametros del servicio (prefijo ticketya.notificaciones). Se ajustan por YAML sin tocar el codigo. */
@Getter
@Setter
@ConfigurationProperties(prefix = "ticketya.notificaciones")
public class NotificacionesProperties {

    /** Direccion "From" de los emails. */
    private String remitente = "no-reply@ticketya.local";

    private Envio envio = new Envio();

    private Executor executor = new Executor();

    @Getter
    @Setter
    public static class Envio {
        /** Intentos reales de envio antes de dejar la notificacion FALLIDA definitiva. */
        private int intentosMaximos = 5;
        /** Espera antes del intento n+1 (el ultimo valor se repite si hay mas intentos que valores). */
        private List<Duration> backoff = new ArrayList<>(List.of(
                Duration.ofSeconds(10), Duration.ofSeconds(30), Duration.ofMinutes(1), Duration.ofMinutes(5)));
        /** Una notificacion ENVIANDO sin actualizarse por este tiempo vuelve a ser elegible. */
        private Duration staleTimeout = Duration.ofMinutes(5);
        /** Espera cuando el circuito esta abierto: waitDurationInOpenState + 5s. No consume intento. */
        private Duration esperaCircuitoAbierto = Duration.ofSeconds(35);
        /** Maximo de notificaciones que toma el worker por pasada (el intervalo es ticketya.notificaciones.envio.poll-ms). */
        private int lote = 20;
    }

    @Getter
    @Setter
    public static class Executor {
        private int core = 2;
        private int max = 4;
        private int cola = 100;
    }
}
