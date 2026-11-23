package com.ticketya.evento.event;

/** Se publica al actualizar un evento cuando cambia su nombre, fecha o lugar. {@code cambios} ya esta descrito en texto. */
public record EventoModificadoEvent(Long idEvento, String cambios) {
}
