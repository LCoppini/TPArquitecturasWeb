package com.ticketya.evento.dto.request;

/** Cuerpo que msvc-entrada espera en {@code POST /internal/entradas/eventos/{idEvento}/modificacion}. */
public record ModificacionEventoRequest(String cambios) {
}
