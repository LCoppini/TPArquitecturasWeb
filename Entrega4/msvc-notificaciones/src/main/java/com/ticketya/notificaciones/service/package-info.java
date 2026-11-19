/**
 * CAPA DE NEGOCIO. Interfaces de los servicios. Las implementaciones van en service/impl.
 * Aca viven las reglas de negocio, las transacciones (@Transactional) y las llamadas a otros microservicios (via client).
 * Es la UNICA capa que usa los repositories.
 *
 * Responsabilidad de este servicio:
 *  - Armar y enviar emails reales (SMTP) a partir de plantillas: compra confirmada, evento cancelado/modificado,
 *    devolucion aprobada y artista favorito que publica un evento en la ciudad del cliente.
 *  - Concentrar la configuracion SMTP, las plantillas y la resiliencia (timeout/retry/circuit breaker) del envio.
 *  - Registrar cada envio (historial) y evitar duplicados ante reintentos.
 */
package com.ticketya.notificaciones.service;
