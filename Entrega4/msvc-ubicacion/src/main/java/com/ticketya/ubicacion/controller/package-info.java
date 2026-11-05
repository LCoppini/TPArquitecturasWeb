/**
 * CAPA DE PRESENTACION. Controllers REST (@RestController) + anotaciones OpenAPI (@Operation, @ApiResponse).
 * Reciben el HTTP, validan la entrada (@Valid) y delegan en el service. SIN logica de negocio y SIN usar repositories.
 *
 * Endpoints previstos:
 *  - LugarController
 *  - SectorController
 *
 * Rutas expuestas por el gateway: /api/lugares/**, /api/sectores/**
 */
package com.ticketya.ubicacion.controller;
