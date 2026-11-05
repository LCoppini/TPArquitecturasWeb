/**
 * CAPA DE PRESENTACION. Controllers REST (@RestController) + anotaciones OpenAPI (@Operation, @ApiResponse).
 * Reciben el HTTP, validan la entrada (@Valid) y delegan en el service. SIN logica de negocio y SIN usar repositories.
 *
 * Endpoints previstos:
 *  - EntradaController: reservar, comprar, mis entradas, devolucion, validar QR (E2)
 *  - PagoController: pasarela mock
 *
 * Rutas expuestas por el gateway: /api/entradas/**, /api/pagos/**
 */
package com.ticketya.entrada.controller;
