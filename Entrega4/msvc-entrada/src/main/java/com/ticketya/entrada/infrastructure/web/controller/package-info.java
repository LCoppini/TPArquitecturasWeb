/**
 * Controllers REST (@RestController) + anotaciones OpenAPI (@Operation, @ApiResponse). Sin logica de negocio.
 * Endpoints previstos:
 *  - EntradaController: reservar, comprar, mis entradas, devolucion, validar QR (E2)
 *  - PagoController: pasarela mock
 * Rutas expuestas por el gateway: /api/entradas/**, /api/pagos/**
 */
package com.ticketya.entrada.infrastructure.web.controller;
