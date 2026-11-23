/**
 * Excepciones del servicio y un @RestControllerAdvice (NotificacionesExceptionHandler) que las traduce a respuestas HTTP
 * uniformes (400, 404, 409). Mismo formato de error (ErrorApiResponse) que el resto de los servicios.
 *
 *  - RecursoNoEncontradoException (404), SolicitudInvalidaException (400: faltan datos del tipo)
 *  - EnvioEmailException y CircuitoAbiertoException: fallos del envio; no llegan al cliente HTTP, las maneja el reintento.
 */
package com.ticketya.notificaciones.exception;
