/**
 * CAPA DE PERSISTENCIA. Entidades JPA (@Entity) y enums del modelo de datos. Base propia: PostgreSQL ticketya_notificaciones.
 *
 * Modelo esperado (propuesta, ajustar en el change):
 *  - Notificacion (id, id_usuario, tipo, destinatario, asunto, estado, intentos, fecha_creacion, fecha_envio)
 *    Historial de envios: sirve para auditar y para no duplicar un envio ante reintentos.
 *  - id_usuario es una REFERENCIA por id (sin FK): los datos del usuario se consultan a msvc-usuarios
 *  - TipoNotificacion (enum): COMPRA_CONFIRMADA, DEVOLUCION_APROBADA, EVENTO_CANCELADO, EVENTO_MODIFICADO, ARTISTA_FAVORITO_PUBLICA_EVENTO
 *  - EstadoNotificacion (enum): PENDIENTE, ENVIADA, FALLIDA
 *
 * Nunca se exponen directamente en la API: se devuelven DTOs.
 */
package com.ticketya.notificaciones.entity;
