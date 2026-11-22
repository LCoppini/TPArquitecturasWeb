/**
 * CAPA DE PERSISTENCIA. Entidades JPA (@Entity) y enums del modelo de datos. Base propia: PostgreSQL ticketya_notificaciones.
 *
 * Modelo:
 *  - Notificacion (id, clave_idempotencia UNIQUE, id_usuario, tipo, destinatario, asunto, datos jsonb, estado, intentos,
 *    proximo_intento_en, ultimo_error, correlation_id, fecha_creacion, fecha_actualizacion, fecha_envio)
 *    Historial de envios: sirve para auditar, reintentar y no duplicar un envio ante reintentos del llamador.
 *  - id_usuario es una REFERENCIA por id (sin FK): el email se consulta a msvc-usuarios si la solicitud no lo trae
 *  - TipoNotificacion (enum): COMPRA_CONFIRMADA, DEVOLUCION_APROBADA, EVENTO_CANCELADO, EVENTO_MODIFICADO
 *    (ARTISTA_FAVORITO_PUBLICA_EVENTO queda fuera: depende del endpoint de artistas de msvc-preferencias)
 *  - EstadoNotificacion (enum): PENDIENTE, ENVIANDO, ENVIADA, FALLIDA, DESCARTADA
 *
 * Nunca se exponen directamente en la API: se devuelven DTOs.
 */
package com.ticketya.notificaciones.entity;
