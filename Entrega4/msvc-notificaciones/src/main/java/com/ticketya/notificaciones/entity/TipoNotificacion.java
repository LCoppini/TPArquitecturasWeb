package com.ticketya.notificaciones.entity;

/**
 * Tipos de notificacion que envia el servicio.
 * ARTISTA_FAVORITO_PUBLICA_EVENTO queda fuera de este change (depende del endpoint de artistas de preferencias).
 */
public enum TipoNotificacion {
    COMPRA_CONFIRMADA,
    DEVOLUCION_APROBADA,
    EVENTO_CANCELADO,
    EVENTO_MODIFICADO
}
