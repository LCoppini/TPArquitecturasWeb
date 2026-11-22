package com.ticketya.notificaciones.exception;

import java.time.Instant;

/** Mismo formato de error que el resto de los servicios. */
public record ErrorApiResponse(Instant fecha, int estado, String error, String mensaje, String ruta) {
}
