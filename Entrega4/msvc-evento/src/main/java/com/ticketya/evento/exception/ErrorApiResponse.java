package com.ticketya.evento.exception;

import java.time.Instant;

public record ErrorApiResponse(Instant fecha, int estado, String error, String mensaje, String ruta) {
}
