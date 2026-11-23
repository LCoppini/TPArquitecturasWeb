package com.ticketya.notificaciones.dto.response;

/** Cuantos clientes distintos recibieron el aviso (uno por usuario, aunque siga a varios artistas). */
public record EventoPublicadoResponse(Long idEvento, int usuariosNotificados) {
}
