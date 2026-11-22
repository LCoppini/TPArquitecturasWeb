/**
 * CAPA DE NEGOCIO. Interfaces de los servicios. Las implementaciones van en service/impl.
 * Aca viven las reglas de negocio, las transacciones y las llamadas a otros microservicios (via client).
 * Es la UNICA capa que usa los repositories.
 *
 *  - NotificacionService: solicitud idempotente (por claveIdempotencia), consulta e historial.
 *  - NotificacionEnvioService: reclamo atomico, envio y registro del resultado (reintentos con backoff).
 *    enviarAsync (@Async) se invoca siempre desde OTRO bean para que el proxy intervenga.
 *  - EnviadorEmail: envio SMTP (JavaMailSender); la variante resiliente agrega Circuit Breaker + TimeLimiter.
 *  - service/impl/PlantillaEmail: plantillas de texto por tipo y datos obligatorios.
 *  - service/impl/NotificacionReintentoWorker: @Scheduled que retoma lo que quedo sin enviar.
 */
package com.ticketya.notificaciones.service;
