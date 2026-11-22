/**
 * CAPA DE PRESENTACION. Controllers REST (@RestController) + anotaciones OpenAPI (@Operation, @ApiResponse).
 * Reciben el HTTP, validan la entrada (@Valid) y delegan en el service. SIN logica de negocio y SIN usar repositories.
 *
 * Contrato INTERNO (sin ruta en el gateway; SecurityConfig deniega /internal/**):
 *  - POST /internal/notificaciones: solicitud de envio (202). Idempotente por claveIdempotencia.
 *  - GET /internal/notificaciones/{id}: estado de una notificacion.
 *  - GET /internal/notificaciones?idUsuario=&estado=: historial (hasta 100, la mas reciente primero).
 */
package com.ticketya.notificaciones.controller;
