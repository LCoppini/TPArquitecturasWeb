/**
 * Configuracion Spring del servicio:
 *  - NotificacionesConfig: @EnableAsync + ThreadPoolTaskExecutor propio ("notificacionesExecutor") con MdcTaskDecorator,
 *    @EnableScheduling para el worker de reintentos y el bean Clock.
 *  - NotificacionesProperties: parametros ticketya.notificaciones.* (remitente, intentos, backoff, stale-timeout, pool).
 *  - CorrelationIdFilter: toma X-Correlation-Id y lo pone en el MDC (se genera uno si falta).
 *  - OpenApiConfig: documentacion OpenAPI.
 * El SMTP (spring.mail.*) y el circuito "email" de Resilience4j (resilience4j.*) se configuran en
 * msvc-config/.../msvc-notificaciones.yml. Las credenciales SMTP van por variables de entorno: nunca en el repositorio.
 */
package com.ticketya.notificaciones.config;
