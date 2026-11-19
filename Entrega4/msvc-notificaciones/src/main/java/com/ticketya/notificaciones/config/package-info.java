/**
 * Configuracion Spring: JavaMailSender (SMTP real, p. ej. Mailtrap), plantillas de email y Resilience4j
 * (timeout, retry, circuit breaker) sobre el envio. Si el envio es asincrono (fire-and-forget): @EnableAsync.
 * Las credenciales SMTP van por variables de entorno: nunca en el repositorio.
 */
package com.ticketya.notificaciones.config;
