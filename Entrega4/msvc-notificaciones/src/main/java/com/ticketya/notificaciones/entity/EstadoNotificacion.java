package com.ticketya.notificaciones.entity;

/**
 * Ciclo de vida de una notificacion.
 * PENDIENTE: registrada o reprogramada, esperando envio.
 * ENVIANDO: reclamada por un hilo (si queda trabada mucho tiempo vuelve a ser elegible).
 * ENVIADA: el servidor SMTP la acepto.
 * FALLIDA: fallo el ultimo intento; se reintenta mientras queden intentos.
 * DESCARTADA: no se puede enviar y no se reintenta (usuario inexistente o sin email).
 */
public enum EstadoNotificacion {
    PENDIENTE,
    ENVIANDO,
    ENVIADA,
    FALLIDA,
    DESCARTADA
}
