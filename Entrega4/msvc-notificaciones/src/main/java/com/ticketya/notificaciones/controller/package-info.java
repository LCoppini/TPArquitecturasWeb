/**
 * CAPA DE PRESENTACION. Controllers REST (@RestController) + anotaciones OpenAPI (@Operation, @ApiResponse).
 * Reciben el HTTP, validan la entrada (@Valid) y delegan en el service. SIN logica de negocio y SIN usar repositories.
 *
 * Endpoints previstos (a definir en el change de OpenSpec):
 *  - POST /api/notificaciones: solicitud de envio. Interna: la invocan msvc-entrada y msvc-evento en modo fire-and-forget
 *  - GET /api/notificaciones?idUsuario=: historial de envios (opcional)
 *
 * El gateway NO expone rutas publicas de este servicio por ahora: lo consumen otros microservicios.
 */
package com.ticketya.notificaciones.controller;
